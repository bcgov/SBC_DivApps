# Copyright © 2021 Province of British Columbia
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Endpoints to check and manage tabs."""

from flask import request, jsonify, redirect, Response
from flask_restx import Namespace
from flask_restx import Resource
from flask_restx import reqparse
from flask_restx import cors
from distutils.util import strtobool
from requests_ntlm import HttpNtlmAuth
import json
import requests
import os
import jwt
import urllib.parse

from api.utilities.cors_util import cors_preflight
from api.auth.auth import jwtcookiemanager

api = Namespace('ssrs', description='Proxy for SSRS embedded reports')

SSRS_SERVER = os.getenv('SSRS_SERVER')
SSRS_BASE_URI = os.getenv('SSRS_BASE_URI')
SSRS_SYSTEM_USER = os.getenv('SSRS_SYSTEM_USER')
SSRS_SYSTEM_CODE = os.getenv('SSRS_SYSTEM_CODE')

DB_FOLDER = os.getenv('DB_FOLDER', '/app/resources')
SSRS_VALIDATION_URI=os.getenv('SSRS_VALIDATION_URI', None) # determine which pages to roles authorization, None means everything will pass

ORIGIN = f'http://{SSRS_SERVER}'
SITE_NAME = f'http://{SSRS_SERVER}/{SSRS_BASE_URI}'
DB_FILE_PATH = f'{DB_FOLDER}{os.path.sep}db.json'

PREFIX = 'Bearer '
COOKIE_PREFIX = 'wtd-rp='


#  http://localhost:5000/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA
#  http://padawan/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA
#  http://tarfful/ReportServer/Pages/ReportViewer.aspx?%2FProject_LAN%20Org%2F%25%20of%20Employees%20meet%20target&rs%3AParameterLanguage=en-CA
#  http://padawan/ReportServer/Pages/ReportViewer.aspx?%2FProject-CCII%2FCCII%20Submissions&rs%3AParameterLanguage=en-CA
#  http://padawan/ReportServer/Pages/ReportViewer.aspx?%2FProject-CCII%2FCCII%20Status&rs%3AParameterLanguage=en-CA
#  http://padawan/ReportServer?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-Service%20BC%20Weekly%20Submission&rs%3AParameterLanguage=en-CA


def get_token(header):
    if not header.startswith(PREFIX):
        raise ValueError('Invalid token')
    return header[len(PREFIX):]

def get_token_from_cookie(headers):
    if headers and 'Cookie' in headers:
        header = headers['Cookie']
        if header:
            cookies = header.split(';')
            if cookies:
                for cookie in cookies:
                    strippedCookie = cookie.strip()
                    if strippedCookie.startswith(COOKIE_PREFIX):
                        return strippedCookie[len(COOKIE_PREFIX):]
    return None

def validateRole(path, requestUrl):
    if not SSRS_VALIDATION_URI:
        return (True, 'No Validation URI set')
    if not os.path.exists(DB_FILE_PATH):
        warnings.warn(f'No authorization validation because file: {DB_FILE_PATH} cannot be found')
        return (True, 'No db.json set')
   
    # Just check the if the inital request has a token. no need to check all the javascript and css files that come back
    if path and path.lower() != SSRS_VALIDATION_URI.lower():
        return (True, 'URL is not meant to be validated')    
        
    token = get_token_from_cookie(request.headers)
    if not token:
        return (False, 'Missing token')
    decoded = jwt.decode(token, verify=False)
    groups = decoded['groups']

    #pthyon requests don't come as https for some reason so rename it before compare
    requestUrl = requestUrl.replace('http://', 'https://')

    encodedRequestUrl = urllib.parse.quote(requestUrl)
    # Fetch json file containing tab/tile info
    f = open (DB_FILE_PATH, "r") 
    # Reading from file 
    data = json.loads(f.read())
    for tab in data['tabs']:
        for tile in tab['tiles']:
            tileUrl = tile['tileURL']
            encodedTileUrl = urllib.parse.quote(tileUrl)
            if encodedTileUrl == encodedRequestUrl:
                tilegroups = tile['tileGroups']
                #Loop through check if role exists in the tile's group, if not remove tile  
                if any(i in tilegroups for i in groups):
                    return (True, 'Authorization found')
                else:
                    return (False, 'Unsufficient permissions')
    return (False, f'Did not find matching URL for: [{requestUrl}]')                        

@cors_preflight('GET,POST,OPTIONS')
@api.route('/<path:path>',methods=['GET','POST','OPTIONS'])
class SSRSProxy(Resource):

    @cors.crossdomain(origin='*')
    @jwtcookiemanager.requires_auth
    def get(self, path):
        (result, message) = validateRole(path, request.url)
        print(f'Endpoint Authorization: {message}')
        if not result:
            return {'error': message}, 401
        query = request.query_string.decode()
        resp = requests.get(f'{SITE_NAME}/{path}?{query}', headers=self.parse_headers(), auth=HttpNtlmAuth(SSRS_SYSTEM_USER, SSRS_SYSTEM_CODE))

        response = Response(resp.content, resp.status_code)

        if resp.headers.get('Content-Type') != None:
            response.headers.set('Content-Type', resp.headers.get('Content-Type'))
        return response

    @cors.crossdomain(origin='*')
    @jwtcookiemanager.requires_auth
    def post(self, path):
        query = request.query_string.decode()
        payload = request.get_data()
        
        # TODO Maybe payload will be different for different queries
        textpayload = f'{payload}'
        textpayload = textpayload[2:-1]

        resp = requests.post(f'{SITE_NAME}/{path}?{query}',data=textpayload, headers=self.parse_headers(), auth=HttpNtlmAuth(SSRS_SYSTEM_USER, SSRS_SYSTEM_CODE))
        
        response = Response(resp.content, resp.status_code)
        return response

    def parse_headers(self):
        excluded_headers = ['referer', 'host', 'origin']
        new_headers = {}
        for (name, value) in request.headers.items():
            new_headers[name] = value
        if request.headers.get('Referer') != None:
            original_referrer = request.headers.get('Referer')
            # print(f'Original Referrer: {original_referrer}, SSRS_BASE: {SSRS_BASE_URI}')
            if SSRS_BASE_URI in original_referrer:
                index = original_referrer.index(SSRS_BASE_URI)
                existing_uri = original_referrer[index + len(SSRS_BASE_URI) + 1:]
                new_referrer = f'http://{SSRS_SERVER}/{SSRS_BASE_URI}/{existing_uri}'
            else:
                new_referrer = f'http://{SSRS_SERVER}'
            new_headers['Referer'] = new_referrer
        if request.headers.get('Host') != None:
            new_headers['Host'] = SSRS_SERVER
        if request.headers.get('Origin') != None:
            new_headers['Origin'] = ORIGIN
        return new_headers
    