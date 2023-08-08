package org.camunda.bpm.extension.hooks.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.camunda.bpm.engine.impl.identity.IdentityProviderException;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.extension.keycloak.KeycloakConfiguration;
import org.camunda.bpm.extension.keycloak.rest.KeycloakRestTemplate;
import org.camunda.bpm.engine.identity.User;
import org.keycloak.admin.client.Keycloak;
// import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.camunda.bpm.extension.keycloak.json.JsonUtil.getJsonString;
import static org.camunda.bpm.extension.keycloak.json.JsonUtil.parseAsJsonArray;

@Qualifier("userService")
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired(required = false)
    private Keycloak keycloak;
    @Value("${keycloak.url.realm}")
    private String keycloakRealm;

    public User searchUserByAttribute(String attributeName, String attributeValue) {
        try {
            List<UserRepresentation> users = keycloak
                    .realm(keycloakRealm)
                    .users()
                    .searchByAttributes(attributeName + ":" + attributeValue);
            UserRepresentation user = users.get(0);
            UserEntity result = new UserEntity();
            String username = user.getUsername();
            String email = user.getEmail();
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            result.setId(username);
            result.setEmail(email);
            result.setFirstName(firstName);
            result.setLastName(lastName);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        }
        return null;
    }
}
