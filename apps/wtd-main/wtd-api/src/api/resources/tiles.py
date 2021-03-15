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
"""Endpoints to check and manage tiles."""

from flask import request, jsonify
from flask_restx import Namespace
from flask_restx import Resource
from flask_restx import cors

from api.auth.auth import jwt

api = Namespace('Tiles', description='API for managing and obtaining tile information')


@api.route('/tile')
class TabManagement(Resource):
    """TileManagement resource."""

    # @jwt.requires_auth
    @cors.crossdomain(origin='*') 
    def get(self):
        """Return a JSON object that identifies if the service is setupAnd ready to work."""
        # TODO: add a poll to the DB when called
        return {'message': 'Tile API ready'}, 200
