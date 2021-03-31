import { KCUserProfile } from 'sbc-common-components/src/models/KCUserProfile'
import { decodeKCToken } from 'sbc-common-components/src/util/common-util'

/** Gets Keycloak roles from KeycloakService. */
export function getKeycloakGroups (): Array<string> {
  const userProfile: KCUserProfile = getKeyCloakUserProfile()
  if (userProfile && userProfile.roles && userProfile.roles.length > 0) {
    return userProfile.roles
  }
  throw new Error('Error getting Keycloak roles')
}

export function getKeyCloakUserProfile (): KCUserProfile {
  const parsedToken: any = decodeKCToken()

  return {
    // eslint-disable-next-line camelcase
    lastName: parsedToken?.family_name,
    // eslint-disable-next-line camelcase
    firstName: parsedToken?.given_name,
    email: parsedToken?.email,
    roles: parsedToken?.groups,
    keycloakGuid: parsedToken?.sub,
    // eslint-disable-next-line camelcase
    userName: parsedToken?.preferred_username,
    fullName: parsedToken?.name,
    loginSource: parsedToken?.loginSource
  }
}
