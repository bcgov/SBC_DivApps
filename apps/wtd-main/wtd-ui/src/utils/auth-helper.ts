import { SessionStorageKeys } from 'sbc-common-components/src/util/constants'
import KeycloakService from 'sbc-common-components/src/services/keycloak.services'
import { KCUserProfile } from 'sbc-common-components/src/models/KCUserProfile'

import { axios } from '@/utils'

/** Gets Keycloak JWT and parses it. */
function getJWT (): any {
  const token = sessionStorage.getItem(SessionStorageKeys.KeyCloakToken)
  if (token) {
    return parseToken(token)
  }
  throw new Error('Error getting Keycloak token')
}

/** Decodes and parses Keycloak token. */
function parseToken (token: string): any {
  try {
    const base64Url = token.split('.')[1]
    const base64 = decodeURIComponent(window.atob(base64Url).split('').map(function (c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    }).join(''))
    return JSON.parse(base64)
  } catch (err) {
    throw new Error('Error parsing Keycloak token - ' + err)
  }
}

/** Gets Keycloak roles from JWT. */
export function getKeycloakRoles (): Array<string> {
  const jwt = getJWT()
  const keycloakRoles = jwt.roles
  if (keycloakRoles && keycloakRoles.length > 0) {
    return keycloakRoles
  }
  return ['']
  // throw new Error('Error getting Keycloak roles')
}

/** Gets Keycloak roles from KeycloakService. */
export function getKeycloakRolesFromService (): Array<string> {
  const userProfile: KCUserProfile = KeycloakService.getUserInfo()
  if (userProfile && userProfile.roles && userProfile.roles.length > 0) {
    return userProfile.roles
  }
  return ['']
  // throw new Error('Error getting Keycloak roles')
}
