package org.camunda.bpm.extension.keycloak.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.identity.IdentityProviderException;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.extension.keycloak.CacheableKeycloakUserQuery;
import org.camunda.bpm.extension.keycloak.KeycloakConfiguration;
import org.camunda.bpm.extension.keycloak.KeycloakContextProvider;
import org.camunda.bpm.extension.keycloak.KeycloakGroupNotFoundException;
import org.camunda.bpm.extension.keycloak.json.JsonException;
import org.camunda.bpm.extension.keycloak.rest.KeycloakRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
//import org.springframework.util.StringUtils;
//import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;



import static org.camunda.bpm.extension.keycloak.json.JsonUtil.*;

/**
 * Keycloak User Service.
 * Custom class for Implementation of user queries against Keycloak's REST API.
 */
public class KeycloakUserService  extends org.camunda.bpm.extension.keycloak.KeycloakUserService {

    /** This class' logger.
    private final Logger LOGGER = Logger.getLogger(KeycloakUserService.class.getName()); */
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakUserService.class);

    private String webClientId;
    private boolean enableClientAuth;
	private boolean enableMultiTenancy;
    private TenantService tenantService;

    public KeycloakUserService(KeycloakConfiguration keycloakConfiguration, KeycloakRestTemplate restTemplate,
                               KeycloakContextProvider keycloakContextProvider, CustomConfig config) {
        super(keycloakConfiguration, restTemplate, keycloakContextProvider);

        this.webClientId = config.getWebClientId();
        this.enableClientAuth = config.isEnableClientAuth();
		this.enableMultiTenancy = config.isEnableMultiTenancy();
        if (this.enableMultiTenancy) {
            this.tenantService = new TenantService(restTemplate, keycloakContextProvider, config);
        }
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

	protected List<User> requestUsersByClientRoleAndTenantId(){
        List<User> userList = new ArrayList<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> claims = null;
        if (authentication instanceof JwtAuthenticationToken) {
            claims = ((JwtAuthenticationToken)authentication).getToken().getClaims();
        } else if (authentication.getPrincipal() instanceof OidcUser) {
            claims = ((OidcUser)authentication.getPrincipal()).getClaims();
        }
        String tenantKey = null;
        if (claims != null && claims.containsKey("tenantKey")) {
            tenantKey = claims.get("tenantKey").toString();
        }
        // IF tenantKey is null, it means user is logging into camunda cockpit. Return empty list.
        if (tenantKey == null)
        	return userList;
        
        try {
            // get Keycloak specific userID
            String keycloakClientID;
            try {
                keycloakClientID = getKeycloakClientID(tenantKey+"-"+webClientId);
            } catch (KeycloakUserNotFoundException e) {
                // user not found: empty search result
                return Collections.emptyList();
            }

            // get groups of this user
            ResponseEntity<String> response = restTemplate.exchange(keycloakConfiguration.getKeycloakAdminUrl()
                            + "/clients/" + keycloakClientID + "/roles/formsflow-reviewer/users", HttpMethod.GET,
                    String.class);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                throw new IdentityProviderException(
                        "Unable to read user data from " + keycloakConfiguration.getKeycloakAdminUrl()
                                + ": HTTP status code " + response.getStatusCodeValue());
            }

            JsonArray searchResult = parseAsJsonArray(response.getBody());
            for (int i = 0; i < searchResult.size(); i++) {
                User user = transformUser(getJsonObjectAtIndex(searchResult, i),tenantKey);
                userList.add(user);
            }

        } catch (HttpClientErrorException hcee) {
            // if userID is unknown server answers with HTTP 404 not found
            if (hcee.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return Collections.emptyList();
            }
            throw hcee;
        } catch (RestClientException | JsonException rce) {
            throw new IdentityProviderException("Unable to query roles for client " + webClientId, rce);
        }
        return userList;
    }

    private UserEntity transformUser(JsonObject result, String prefix) throws JsonException {
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