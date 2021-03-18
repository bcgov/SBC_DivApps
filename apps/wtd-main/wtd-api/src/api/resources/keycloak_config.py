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

from flask import request, jsonify
from flask_restx import Namespace
from flask_restx import Resource
from flask_restx import reqparse
from flask_restx import cors
import json

from api.auth.auth import jwt

api = Namespace('config', description='API for returning inital dashboard configuration')

filepath = 'src/api/resources/keycloak_config.json'


@api.route('/keycloak.json')
class DashboardAuthConfig(Resource):

    @cors.crossdomain(origin='*')
    def get(self):
        """Return a JSON object of tab and tile information"""
        # Fetch json file containing tab/tile info
        f = open (filepath, "r") 
        # Reading from file 
        data = json.loads(f.read())
        return data, 200
