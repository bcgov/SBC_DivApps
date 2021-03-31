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
"""Map and fetch wait time map information"""

from flask import request, jsonify, redirect, Response
from flask_restx import Namespace
from flask_restx import Resource
from flask_restx import reqparse
from flask_restx import cors
from api.utilities.cors_util import cors_preflight
import json
import requests
import os

from api.auth.auth import jwtmanager

api = Namespace('Map', description='Map and fetch wait time map information')

filepath = 'src/api/resources/service-bc-offices.json'
sampledata = 'src/api/resources/samplemapdata.json'
mapconfig = 'src/api/resources/map-config.json'

WAIT_TIME_API_URL = os.getenv('WAIT_TIME_API', 'https://api.analytics.gov.bc.ca/SBC-RT')
# The URL of this API service, needed to be placed in the map-config.json
WAIT_TIME_MAP_SERVER = os.getenv('SERVER_NAME', 'WAIT_TIME_MAP_UNDEFINED')
MAP_USE_HTTPS = os.getenv('MAP_USE_HTTPS', 'True')

@cors_preflight('GET,OPTIONS')
@api.route('/wait-time',methods=['GET'])
class WaitTimeMap(Resource):

    @cors.crossdomain(origin='*')
    def get(self):
        #Need to sent get to wait time route below and use response instead of sample data
        resp = requests.get(WAIT_TIME_API_URL)
        #response = Response(resp.content, resp.status_code)
        f = open (filepath, "r")
        #g = open (sampledata, "r") 
        # Reading from file 
        data = json.loads(f.read())
        response = json.loads(resp.content)

        officelist=[]
        for office in data['features']:
            for officedata in response['data']:
                if office['id'] == str(officedata['office_id']):
                    office['properties']['WAIT_TIME'] = officedata['estimated_wait']
                    office['properties']['WAIT_NUM'] = officedata['current_line_length']
            officelist.append(office)
        data['features'] = officelist

        return data

@cors_preflight('GET,OPTIONS')
@api.route('/config',methods=['GET'])
class MapConfig(Resource):

    @cors.crossdomain(origin='*')
    def get(self):
        """Return a JSON object of tab and tile information"""
        # Fetch json file containing tab/tile info
        f = open (mapconfig, "r") 
        # Reading from file 
        data = json.loads(f.read())
        if (MAP_USE_HTTPS == 'True'):
            data['layers'][0]['dataUrl'] = f'https://{WAIT_TIME_MAP_SERVER}/wtd/api/v1/map/wait-time'
        else:
            data['layers'][0]['dataUrl'] = f'http://{WAIT_TIME_MAP_SERVER}/wtd/api/v1/map/wait-time'
        return data, 200
        