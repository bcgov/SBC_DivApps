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
from jsonschema import validate
from api.utilities.cors_util import cors_preflight
from shutil import copyfile

import json
import jwt
import os

from api.auth.auth import jwtmanager

api = Namespace('Tabs', description='API for managing and obtaining tab information')

EDIT_GROUP = os.getenv('EDIT_GROUP')
DB_FOLDER = os.getenv('DB_FOLDER', '/app/resources')

BASE_FILE = 'src/api/resources/db.json'

DB_FILE_PATH = f'{DB_FOLDER}{os.path.sep}db.json'
PREFIX = 'Bearer '

DB_JSON_SCHEMA = {
    "$schema": "http://json-schema.org/draft-04/schema#",
    "type": "object",
    "properties": {
        "tabs": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "tabName": {"type": "string"},
                    "tabOrder": {"type": "integer"},
                    "tiles": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "tileGroups": {
                                    "type": "array",
                                    "items": {"type": "string"}
                                },
                                "tileName": {"type": "string"},
                                "tileOrder": {"type": "integer"},
                                "tileType": {"type": "string"},
                                "tileURL": {"type": "string"}
                            },
                            "required": [
                                "tileName",
                                "tileOrder",
                                "tileType"
                            ]
                        }
                    }
                },
                "required": [
                    "tabName",
                    "tabOrder",
                ]
            }
        }
    },
    "required": [
        "tabs"
    ]
}

def get_token(header):
    if not header.startswith(PREFIX):
        raise ValueError('Invalid token')

    return header[len(PREFIX):]

@cors_preflight('GET,POST,OPTIONS')
@api.route('/tabs', methods=['GET', 'POST', 'OPTIONS'])
class TabManagement(Resource):
    """TabManagement resource."""

    @cors.crossdomain(origin='*')
    @jwtmanager.requires_auth
    def get(self):
        """Return a JSON object of tab and tile information"""
        if not os.path.exists(DB_FILE_PATH):
            copyfile(BASE_FILE, DB_FILE_PATH)
        # Fetch json file containing tab/tile info
        f = open (DB_FILE_PATH, "r") 
        # Reading from file 
        data = json.loads(f.read())

        token = get_token(request.headers['Authorization'])
        decoded = jwt.decode(token, verify=False)
        groups = decoded['groups']
        #Remove Tiles if user doesn't have group
        for tab in data['tabs']:
            tilelist = []
            for tile in tab['tiles']:
                tilegroups = tile['tileGroups']
                #Loop through check if role exists in the tile's group, if not remove tile  
                if any(i in tilegroups for i in groups) == True:
                    tilelist.append(tile)
            tab['tiles'] = tilelist
        #Remove empty tabs from view
        tablist = []   
        for tab in data['tabs']:
            if len(tab['tiles']) != 0:
                tablist.append(tab)
        data['tabs'] = tablist
        return data, 200


    @cors.crossdomain(origin='*')
    @jwtmanager.requires_auth
    def post(self):
        """POST a JSON object of tab and tile information"""
        # Fetch json file containing tab/tile info
        token = get_token(request.headers['Authorization'])
        decoded = jwt.decode(token, verify=False)
        groups = decoded['groups']
        if EDIT_GROUP in groups:
            data = request.get_json(force=True)
            # Make sure that the data is valid json and fits the schema
            validate(instance=data, schema=DB_JSON_SCHEMA)

            #verify = json.loads(data)
            f = open (DB_FILE_PATH, "w") 
            f.seek(0)
            # Reading from file 
            f.write(json.dumps(data))
            return data, 200
        else:
            return {'error': 'Unsufficient keycloak group permissions'}, 401

@cors_preflight('GET,OPTIONS')
@api.route('/tabs/edit', methods=['GET', 'OPTIONS'])
class TabEditManagement(Resource):

    @cors.crossdomain(origin='*')
    @jwtmanager.requires_auth
    def get(self):
        """Return a JSON object of tab and tile information."""
        # Fetch json file containing tab/tile info
        token = get_token(request.headers['Authorization'])
        decoded = jwt.decode(token, verify=False)
        groups = decoded['groups']
        f = open (DB_FILE_PATH, "r") 
        # Reading from file
        if EDIT_GROUP in groups: 
            data = json.loads(f.read())
            return data
        else:
            return {'error': 'Unsufficient keycloak group permissions'}, 401

        



