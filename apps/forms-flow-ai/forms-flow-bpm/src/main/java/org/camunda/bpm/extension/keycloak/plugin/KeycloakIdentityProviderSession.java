package org.camunda.bpm.extension.keycloak.plugin;

import java.util.Collections;
import java.util.List;

import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.extension.keycloak.*;
import org.camunda.bpm.extension.keycloak.cache.QueryCache;
import org.camunda.bpm.extension.keycloak.rest.KeycloakRestTemplate;
import org.camunda.bpm.extension.keycloak.util.KeycloakPluginLogger;
import org.springframework.util.StringUtils;

/**
 * @author aot
 *
 */
public class KeycloakIdentityProviderSession
		extends org.camunda.bpm.extension.keycloak.KeycloakIdentityProviderSession {

	public KeycloakIdentityProviderSession(KeycloakConfiguration keycloakConfiguration, KeycloakRestTemplate restTemplate, KeycloakContextProvider keycloakContextProvider,
			QueryCache<CacheableKeycloakUserQuery, List<User>> userQueryCache, QueryCache<CacheableKeycloakGroupQuery, List<Group>> groupQueryCache,
			QueryCache<CacheableKeycloakCheckPasswordCall, Boolean> checkPasswordCache,
			String webClientId, boolean enableClientAuth) {
		super(keycloakConfiguration, restTemplate, keycloakContextProvider, userQueryCache, groupQueryCache, checkPasswordCache);
		this.groupService = new  KeycloakGroupService(keycloakConfiguration, restTemplate, keycloakContextProvider, webClientId, enableClientAuth);
        this.userService = new KeycloakUserService(keycloakConfiguration, restTemplate, keycloakContextProvider, webClientId, enableClientAuth);
	}

	/**
	 * Get the group ID of the configured admin group. Enable configuration using group path as well.
	 * This prevents common configuration pitfalls and makes it consistent to other configuration options
	 * like the flag 'useGroupPathAsCamundaGroupId'.
	 * 
	 * @param configuredAdminGroupName the originally configured admin group name
	 * @return the corresponding keycloak group ID to use: either internal keycloak ID or path, depending on config
	 * 
	 * @see org.camunda.bpm.extension.keycloak.KeycloakGroupService#getKeycloakAdminGroupId(java.lang.String)
	 */
	public String getKeycloakAdminGroupId(String configuredAdminGroupName) {
		return groupService.getKeycloakAdminGroupId(configuredAdminGroupName);
	}

    /**
	 *
	 * @param userQuery
	 * @return
	 */
	protected List<User> findUserByQueryCriteria(KeycloakUserQuery userQuery) {
		StringBuilder resultLogger = new StringBuilder();
		if (KeycloakPluginLogger.INSTANCE.isDebugEnabled()) {
			resultLogger.append("Keycloak group query results: [");
		}

		List<User> allMatchingUsers = userQueryCache.getOrCompute(CacheableKeycloakUserQuery.of(userQuery),
				this::doFindUserByQueryCriteria);
		List<User> processedUsers = userService.postProcessResults(userQuery, allMatchingUsers, resultLogger);
		if (KeycloakPluginLogger.INSTANCE.isDebugEnabled()) {
			resultLogger.append("]");
			KeycloakPluginLogger.INSTANCE.groupQueryResult(resultLogger.toString());
		}

		return processedUsers;
	}

    /**
	 *
	 * @param userQuery
	 * @return
	 */
	private List<User> doFindUserByQueryCriteria(CacheableKeycloakUserQuery userQuery) {
		return StringUtils.hasLength(userQuery.getGroupId()) ?
				this.userService.requestUsersByGroupId(userQuery) :
				this.userService.requestUsersWithoutGroupId(userQuery);
	}
	
}