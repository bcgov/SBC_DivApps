# Copyright © 2019 Province of British Columbia
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

from flask import request, jsonify, g
from flask_restx import Namespace
from flask_restx import Resource
from flask_restx import reqparse
from flask_restx import cors
from api.utilities.cors_util import cors_preflight

import json
import jwt

from api.auth.auth import jwtmanager

api = Namespace('', description='API for managing and obtaining tab information')

#filepath = 'C:/Users/josh.colaco/Documents/WaitTimeDashboard/wtd/SBC_DivApps/apps/wtd-main/wtd-api/src/api/resources/db.json'
filepath = 'src/api/resources/db.json'
PREFIX = 'Bearer '

def get_token(header):
    if not header.startswith(PREFIX):
        raise ValueError('Invalid token')

    return header[len(PREFIX):]

@cors_preflight('GET,POST,OPTIONS')
@api.route('/Tabs', methods=['GET', 'POST', 'OPTIONS'])
class TabManagement(Resource):
    """TabManagement resource."""

    @cors.crossdomain(origin='*')
    @jwtmanager.requires_auth
    def get(self):
        """Return a JSON object of tab and tile information"""
        # Fetch json file containing tab/tile info
        f = open (filepath, "r") 
        # Reading from file 
        data = json.loads(f.read())
        token = get_token(request.headers['Authorization'])
        decoded = jwt.decode(token, verify=False)
        groups = decoded['groups']
        for _ in range(1000):
            for tab in data['tabs']:
                for tile in tab['tiles']:
                    tilegroups = tile['tileGroups']
                    #loop through check if role exists in the tile's group, if not remove tile               
                    if any(i in tilegroups for i in groups) == False:
                        tab['tiles'].remove(tile)
        for _ in range(1000):
            for tab in data['tabs']:
                if not 'tiles' in tab or len(tab['tiles']) == 0:
                    data['tabs'].remove(tab)
        return data, 200


    @cors.crossdomain(origin='*')
    @jwtmanager.requires_auth
    def post(self):
        """POST a JSON object of tab and tile information"""
        # Fetch json file containing tab/tile info
        data = request.get_json(force=True)
        verify = json.loads(data)
        f = open (filepath, "w") 
        f.seek(0)
        # Reading from file 
        f.write(json.dumps(data))
        return data, 200



