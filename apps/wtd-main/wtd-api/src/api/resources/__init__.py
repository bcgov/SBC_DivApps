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

# from .trace import API as TRACE_API
from .tabs import api as TABS_API
from .tiles import api as TILES_API
from .dashboard_config import api as DASHBOARD_CONFIG_API
from .keycloak_config import api as KEYCLOAK_CONFIG_API
from .ssrs_reverse_proxy import api as SSRS_PROXY_API
import os

SSRS_BASE_URI = os.getenv('SSRS_BASE_URI')

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

API.add_namespace(TABS_API, path='/api/v1')
API.add_namespace(TILES_API, path='/api/v1/tile')
API.add_namespace(DASHBOARD_CONFIG_API, path='/api/v1/config')
API.add_namespace(KEYCLOAK_CONFIG_API, path='/api/v1/config')
API.add_namespace(SSRS_PROXY_API, path=f'/{SSRS_BASE_URI}')
