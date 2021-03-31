
from flask_jwt_oidc import JwtManager


class JwtCookieManager(JwtManager):
    @staticmethod
    def method_two():
        print('sd')

    @staticmethod
    def get_token_auth_header():
        print('-------------------__OVERRIDE------------')
        """Obtain the access token from the Authorization Header."""
        auth = request.headers.get('Authorization', None)
        if not auth:
            raise AuthError({'code': 'authorization_header_missing',
                             'description': 'Authorization header is expected'}, 401)

        parts = auth.split()

        if parts[0].lower() != 'bearer':
            raise AuthError({'code': 'invalid_header',
                             'description': 'Authorization header must start with Bearer'}, 401)

        if len(parts) < 2:
            raise AuthError({'code': 'invalid_header',
                             'description': 'Token not found after Bearer'}, 401)

        if len(parts) > 2:
            raise AuthError({'code': 'invalid_header',
                             'description': 'Authorization header is an invalid token structure'}, 401)

        return parts[1]