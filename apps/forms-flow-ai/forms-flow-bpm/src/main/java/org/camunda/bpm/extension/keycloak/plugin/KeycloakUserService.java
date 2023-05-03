package org.camunda.bpm.extension.keycloak.plugin;

import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.extension.keycloak.KeycloakConfiguration;
import org.camunda.bpm.extension.keycloak.rest.KeycloakRestTemplate;
import org.camunda.bpm.extension.keycloak.KeycloakContextProvider;
import org.camunda.bpm.extension.keycloak.KeycloakGroupNotFoundException;
import org.camunda.bpm.extension.keycloak.CacheableKeycloakUserQuery;
import org.camunda.bpm.engine.impl.identity.IdentityProviderException;
import org.camunda.bpm.extension.keycloak.json.JsonException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.util.StringUtils;

import java.util.logging.Logger;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static org.camunda.bpm.extension.keycloak.json.JsonUtil.*;

/**
 * Keycloak User Service.
 * Custom class for Implementation of user queries against Keycloak's REST API.
 */
public class KeycloakUserService  extends org.camunda.bpm.extension.keycloak.KeycloakUserService {

    /** This class' logger. */
    private final Logger LOGGER = Logger.getLogger(KeycloakUserService.class.getName());

    private String webClientId;
    private boolean enableClientAuth;

    public KeycloakUserService(KeycloakConfiguration keycloakConfiguration, KeycloakRestTemplate restTemplate,
                               KeycloakContextProvider keycloakContextProvider,String webClientId,boolean enableClientAuth) {
        super(keycloakConfiguration, restTemplate, keycloakContextProvider);
        this.webClientId = webClientId;
        this.enableClientAuth = enableClientAuth;
    }

    @Override
    public List<User> requestUsersByGroupId(CacheableKeycloakUserQuery query) {
        String groupId = query.getGroupId();
		List<User> userList = new ArrayList<>();

		try {
			//  get Keycloak specific groupID
			String keyCloakID;
			try {
				keyCloakID = getKeycloakGroupID(groupId);
			} catch (KeycloakGroupNotFoundException e) {
				// group not found: empty search result
				return Collections.emptyList();
			}

			// get members of this group
			ResponseEntity<String> response = restTemplate.exchange(
					keycloakConfiguration.getKeycloakAdminUrl() + "/groups/" + keyCloakID + "/members?max=" + getMaxQueryResultSize(), 
					HttpMethod.GET, String.class);
			if (!response.getStatusCode().equals(HttpStatus.OK)) {
				throw new IdentityProviderException(
						"Unable to read group members from " + keycloakConfiguration.getKeycloakAdminUrl()
								+ ": HTTP status code " + response.getStatusCodeValue());
			}

			JsonArray searchResult = parseAsJsonArray(response.getBody());
			for (int i = 0; i < searchResult.size(); i++) {
				JsonObject keycloakUser = getJsonObjectAtIndex(searchResult, i);
				if (keycloakConfiguration.isUseEmailAsCamundaUserId() && 
						!StringUtils.hasLength(getJsonString(keycloakUser, "email"))) {
					continue;
				}
				if (keycloakConfiguration.isUseUsernameAsCamundaUserId() &&
						!StringUtils.hasLength(getJsonString(keycloakUser, "username"))) {
					continue;
				}
				userList.add(transformUser(keycloakUser));
			}

		} catch (HttpClientErrorException hcee) {
			// if groupID is unknown server answers with HTTP 404 not found
			if (hcee.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				return Collections.emptyList();
			}
			throw hcee;
		} catch (RestClientException | JsonException rce) {
			throw new IdentityProviderException("Unable to query members of group " + groupId, rce);
		}

		return userList;
    }

    private UserEntity transformUser(JsonObject result) throws JsonException {
        UserEntity user = new UserEntity();
        String userId = getJsonString(result, "username");
        JsonObject attributes =  getJsonObject(result, "attributes");
        if(attributes != null) {
            JsonArray userIds = attributes.getAsJsonArray("userid");
            if(userIds != null) {
                userId = userIds.get(0).getAsString();
            }
        }
        
        String email = getJsonString(result, "email");
        String firstName = getJsonString(result, "firstName");
        String lastName = getJsonString(result, "lastName");
        user.setId(userId);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}