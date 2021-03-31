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

from api.utilities.cors_util import cors_preflight
from api.auth.auth import jwtmanager

api = Namespace('ssrs', description='Proxy for SSRS embedded reports')

SSRS_SERVER = os.getenv('SSRS_SERVER')
SSRS_BASE_URI = os.getenv('SSRS_BASE_URI')
SSRS_SYSTEM_USER = os.getenv('SSRS_SYSTEM_USER')
SSRS_SYSTEM_CODE = os.getenv('SSRS_SYSTEM_CODE')

ORIGIN = f'http://{SSRS_SERVER}'
SITE_NAME = f'http://{SSRS_SERVER}/{SSRS_BASE_URI}'

PREFIX = 'Bearer '
COOKIE_PREFIX = 'wtd-rp='
SSRS_ACCESS_GROUP = os.getenv('SSRS_ACCESS_GROUP', 'SBC Staff')


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

@cors_preflight('GET,POST,OPTIONS')
@api.route('/<path:path>',methods=['GET','POST','OPTIONS'])
class SSRSProxy(Resource):

    @cors.crossdomain(origin='*')
    # @jwtmanager.requires_auth
    def get(self, path):
        if path and path.lower() == 'pages/reportviewer.aspx':
            # Just check the if the inital request has a token. no need to check all the javascript and css files that come back
            token = get_token_from_cookie(request.headers)
            if not token:
                return {'error': 'Missing token'}, 401
            decoded = jwt.decode(token, verify=False)
            groups = decoded['groups']
            print(f'Groups are: {groups}')
            if SSRS_ACCESS_GROUP not in groups:
                return {'error': 'Unsufficient permissions'}, 401
        query = request.query_string.decode()
        resp = requests.get(f'{SITE_NAME}/{path}?{query}', headers=self.parse_headers(), auth=HttpNtlmAuth(SSRS_SYSTEM_USER, SSRS_SYSTEM_CODE))

        response = Response(resp.content, resp.status_code)

        if resp.headers.get('Content-Type') != None:
            response.headers.set('Content-Type', resp.headers.get('Content-Type'))
        return response

    @cors.crossdomain(origin='*')
    # @jwtmanager.requires_auth
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
    