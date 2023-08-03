package org.camunda.bpm.extension.commons.connector.support;

import org.camunda.bpm.extension.commons.ro.req.IRequest;
import org.camunda.bpm.extension.commons.ro.res.IResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

public abstract class AbstractAccessHandler {

    @Override
    public ResponseEntity<String> exchange(String url, HttpMethod method, Map<String, Object> queryParams,
            IRequest payload) {
        return null;
    }

    public String getUserBasedAccessToken() {

        String token = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            token = jwtAuthenticationToken.getToken().getTokenValue();
        }
        return token;
    }
}
