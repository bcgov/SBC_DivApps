"""API endpoints for managing application resource."""

from http import HTTPStatus

from flask import g, jsonify, request
from flask_restx import Namespace, Resource, cors
from ..utils.util import cors_preflight
import json


API = Namespace('HealthCheck', description='HealthCheck')

@cors_preflight('GET,OPTIONS')
@API.route('/healthcheck', methods=['GET', 'OPTIONS'])
class HealthCheck(Resource):
    """Resource for health check of API"""

    @staticmethod
    @cors.crossdomain(origin='*')
    def get(application_id):

        return HTTPStatus.OK
