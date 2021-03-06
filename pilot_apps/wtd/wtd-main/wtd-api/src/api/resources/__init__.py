# Copyright © 2019 Province of British Columbia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Exposes all of the resource endpoints mounted in Flask-Blueprint style.

Uses restplus namespaces to mount individual api endpoints into the service.

All services have 2 defaults sets of endpoints:
 - ops
 - metaTEMPLATE_FOLDER_PATH = None
That are used to expose operational health information about the service, and meta information.
"""

from flask_restx import Api
from .tabs import api as TABS_API
from .waittimemap import api as WAITTIMEMAP_API
from .meta import api as META_API
from .ops import api as OPS_API
import os

# This will add the Authorize button to the swagger docs
# TODO oauth2 & openid may not yet be supported by restplus <- check on this
AUTHORIZATIONS = {
    'apikey': {
        'type': 'apiKey',
        'in': 'header',
        'name': 'Authorization'
    }
}

API = Api(
    title='WTD API',
    version='1.0',
    description='WaitTimeDashboard API for Service BC',
    prefix='/wtd',
    security=['apikey'],
    authorizations=AUTHORIZATIONS)

API.add_namespace(OPS_API, path='/api/v1')
API.add_namespace(META_API, path='/api/v1')
API.add_namespace(TABS_API, path='/api/v1')
API.add_namespace(WAITTIMEMAP_API, path='/api/v1/map')
