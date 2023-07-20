package org.camunda.bpm.extension.hooks.services;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.camunda.bpm.engine.impl.identity.IdentityProviderException;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.extension.keycloak.KeycloakConfiguration;
import org.camunda.bpm.extension.keycloak.rest.KeycloakRestTemplate;
import org.camunda.bpm.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static org.camunda.bpm.extension.keycloak.json.JsonUtil.getJsonString;
import static org.camunda.bpm.extension.keycloak.json.JsonUtil.parseAsJsonArray;

@Qualifier("userService")
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private KeycloakRestTemplate restTemplate;
    private KeycloakConfiguration keycloakConfiguration;
    public UserService(KeycloakConfiguration keycloakConfiguration, KeycloakRestTemplate restTemplate){
        this.restTemplate = restTemplate;
        this.keycloakConfiguration = keycloakConfiguration;
    }

    public User searchUserByAttribute(String attributeName, String attributeValue) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(keycloakConfiguration.getKeycloakAdminUrl()
                            + "/users/?q="+attributeName+":"+attributeValue+"", HttpMethod.GET,
                    String.class);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                throw new IdentityProviderException(
                        "Unable to read user data from " + keycloakConfiguration.getKeycloakAdminUrl()
                                + ": HTTP status code " + response.getStatusCodeValue());
            }
            JsonArray resultList = parseAsJsonArray(response.getBody());
            JsonObject result = resultList.get(0).getAsJsonObject();
            UserEntity user = new UserEntity();
            String username = getJsonString(result, "username");
            String email = getJsonString(result, "email");
            String firstName = getJsonString(result, "firstName");
            String lastName = getJsonString(result, "lastName");
            user.setId(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return user;
        }catch(Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
        return null;
    }
}
