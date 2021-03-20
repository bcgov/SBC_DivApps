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
from requests_ntlm import HttpNtlmAuth
import json
import requests

from api.auth.auth import jwt

api = Namespace('ssrs', description='Proxy for SSRS embedded reports')

filepath = 'src/api/resources/dashboard_config.json'
SITE_NAME = 'http://padawan/ReportServer'
#  http://localhost:5000/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA
#  http://padawan/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA

@api.route('/<path:path>',methods=['GET','POST','DELETE'])
class SSRSProxy(Resource):

    @cors.crossdomain(origin='*')
    def get(self, path):
        query = request.query_string.decode()
        print(f'----> GET  REDIRECT:   {SITE_NAME}/{path}?{query}')
        resp = requests.get(f'{SITE_NAME}/{path}?{query}', headers=self.parse_headers(), auth=HttpNtlmAuth('IDIR\\ROBLO','M!s2wpm1m1'))
        response = Response(resp.content, resp.status_code)

        new_headers = self.copy_headers_to_client(resp.headers)
        if resp.headers.get('Content-Type') != None:
            response.headers.set('Content-Type', resp.headers.get('Content-Type'))
        return response

    @cors.crossdomain(origin='*')
    def post(self, path):
        query = request.query_string.decode()
        payload = request.get_data()
        print(f'----> POST REDIRECT:   {SITE_NAME}/{path}?{query}')
        # print(payload)  
        textpayload = f'{payload}'
        textpayload = textpayload[2:-1]
        # print(textpayload)

        content_len = int(request.headers.get('content-length', 0))
        print(f'---------content length {content_len}')
        post_body = {
            f'{textpayload}': None
        }
        
        kwargs = {'ChannelId': 'channel_id',
            'ReferenceNumber': 'reference_number'
        }
        

        

        # new_cookies = [(name, value) for (name, value) in request.cookies ]

        # print(f'cookies: {new_cookies}')
        # resp = requests.post(f'{SITE_NAME}{path}?{query}',data=textpayload, headers=self.parse_headers(), auth=HttpNtlmAuth('IDIR\\ROBLO','M!s2wpm1m1'))
        resp = requests.post(f'{SITE_NAME}/{path}?{query}',data=textpayload, headers=self.parse_headers(), auth=HttpNtlmAuth('IDIR\\ROBLO','M!s2wpm1m1'))
        
        response = Response(resp.content, resp.status_code)

        # if response.status_code == 401:
        #     print('trying again POST')
        #     resp = requests.post(f'{SITE_NAME}{path}?{query}',data=textpayload, auth=HttpNtlmAuth('IDIR\\ROBLO','M!s2wpm1m1'))
        #     response = Response(resp.content, resp.status_code, self.build_headers())
        # else:
        #     print('PASSED - POST')

        return response


    # def copy_headers_to_server(self, old_headers):
    #     if 'Origin' in old_headers.items()
    #         origin = old_headers['Origin']
    #     if 'Referer' in old_headers
    #         print('referrer found')
    #         referer = old_headers['Referer']
    #     if 'Host' in old_headers
    #         host = old_headers['Host']
    #     new_headers = {}
    #     for (name, value) in old_headers.items():
    #         new_headers[name] = value

    #     if 'Origin' in old_headers
    #         new_headers['Origin'] = f'{origin}'
    #     if 'Referer' in old_headers
    #         new_headers['Referer'] = f'{referer}'
    #     if 'Host' in old_headers
    #         new_headers['Host'] = f'{host}'
    #     return new_headers

    def copy_headers_to_client(self, old_headers):
        new_headers = {}
        for (name, value) in old_headers.items():
            new_headers[name] = value

        if  old_headers.get('Origin') != None:
            new_headers['Origin'] = 'localhost'
        if old_headers.get('Referer') != None:
            new_headers['Referer'] = 'http://localhost:5000/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA'
        if old_headers.get('Host') != None:
            new_headers['Host'] = 'http://localhost'
        return new_headers


    def build_headers(self):
        # print(f'Original Headers: {request.headers.items}')
        excluded_headers = ['referer', 'host', 'origin']
        new_headers = [(name, value) for (name, value) in request.headers.items() if name.lower() not in excluded_headers]
        return self.inject_headers(new_headers)
        
    def inject_headers(self, new_headers):
        new_headers.append(('Host','padawan'))
        new_headers.append(('Origin', 'http://padawan'))
        new_headers.append(('Referer', 'http://padawan/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA'))
        # print(f'New Headers: {new_headers}')
        return new_headers
  
    def parse_headers(self):
        excluded_headers = ['referer', 'host', 'origin']
        new_headers = {}
        for (name, value) in request.headers.items():
            new_headers[name] = value
        new_headers['Referer'] = 'http://padawan/ReportServer/Pages/ReportViewer.aspx?%2FProject-COVID_SI%2FCOVID%20SI%20Plan%20-%20Director%20Dashboard&rs%3AParameterLanguage=en-CA'
        new_headers['Origin'] = 'http://padawan'
        return new_headers
    
    def inject_auth(self, headers):
        headers['Authorizaion'] = 'Bearer secret'
        return headers