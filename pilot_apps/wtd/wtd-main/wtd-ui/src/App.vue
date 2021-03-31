<template>
  <v-app class="app-container" id="app">
    <!-- Dialogs -->
    <account-authorization-dialog
      attach="#app"
      :dialog="accountAuthorizationDialog"
      @retry="initApp()"
    />

    <fetch-error-dialog
      attach="#app"
      :dialog="fetchErrorDialog"
      @exit="closeErrorDialogues()"
      @retry="initApp()"
    />

    <save-error-dialog
      attach="#app"
      :dialog="saveErrorDialog"
      :errors="saveErrors"
      :warnings="saveWarnings"
      @exit="initApp()"
      @okay="saveErrorDialog = false"
    />

    <!-- Initial Page Load Transition -->
    <transition name="fade">
      <div class="loading-container" v-show="!haveData">
        <div class="loading__content">
          <v-progress-circular color="primary" size="50" indeterminate />
          <div class="loading-msg">Loading</div>
        </div>
      </div>
    </transition>

    <wtd-header
        class="sbc-header"
        :inAuth="false"
        :redirectOnLoginSuccess="baseUrl"
        :redirectUrlLoginFail="registryUrl"
        :redirectOnLogout="registryUrl"
        :showActions="true"
        :username=userName
      />

    <div class="app-body">
      <main v-if="!isErrorDialog">
        <v-container class="view-container py-0">
          <router-view
            :appReady=appReady
            :dashboards=dashboards
            :keyCloakGroups=keyCloakGroups
            :registryUrl="registryUrl"
            @profileReady="profileReady = true"
            @updateDashboards="updateDashboardDetails"
            @getUpdateDashboards="getEditDashboard"
            @haveData="haveData = true"
            @haveChanges="stateChangeHandler($event)"
          />
        </v-container>
      </main>
    </div>

    <sbc-footer :aboutText=aboutText />
  </v-app>
</template>

<script lang="ts">
// Libraries
import { Component, Watch, Mixins } from 'vue-property-decorator'
import { Action, Getter } from 'vuex-class'
import KeycloakService from 'sbc-common-components/src/services/keycloak.services'
import { KCUserProfile } from 'sbc-common-components/src/models/KCUserProfile' // eslint-disable-line no-unused-vars
import { getKeyCloakUserProfile, axios } from '@/utils'
import AccountOverrideModule from '@/overrides/account-override'

// Components
import WtdHeader from './components/wtd-header.vue'
import SbcFooter from 'sbc-common-components/src/components/SbcFooter.vue'
import SbcAuthenticationOptionsDialog from 'sbc-common-components/src/components/SbcAuthenticationOptionsDialog.vue'

import * as Dialogs from '@/components/dialogs'
import * as Views from '@/views'

// Mixins, interfaces, etc
import { AuthMixin } from '@/mixins'
import { ActionBindingIF, DashboardTabIF, DashboardPayloadIF } from '@/interfaces' // eslint-disable-line no-unused-vars

// Enums and Constants
import { SessionStorageKeys } from 'sbc-common-components/src/util/constants'

@Component({
  components: {
    WtdHeader,
    SbcFooter,
    SbcAuthenticationOptionsDialog,
    ...Dialogs,
    ...Views
  }
})
export default class App extends Mixins(AuthMixin) {
  // Global getters
  @Getter getUserEmail!: string
  @Getter getUserFirstName!: string
  @Getter getUserLastName!: string
  @Getter getUserUsername!: string

  // Global setter
  @Action setDashboard: ActionBindingIF
  @Action setAccountInformation!: ActionBindingIF
  @Action setUserInfo: ActionBindingIF

  // Local Properties
  private accountAuthorizationDialog: boolean = false
  private fetchErrorDialog: boolean = false
  private saveErrorDialog: boolean = false
  private saveErrors: Array<object> = []
  private saveWarnings: Array<object> = []
  private dashboards: DashboardTabIF[] = null
  private keyCloakGroups: string[] = null

  // FUTURE: change profileReady/appReady/haveData to a state machine?

  /** Whether the user profile is ready (ie, auth is loaded) and we can init the app. */
  private profileReady: boolean = false

  private userName: string = 'Hi'

  /** Whether the app is ready and the views can now load their data. */
  private appReady: boolean = false

  /** Whether the views have loaded their data and the spinner can be hidden. RLO changed this */
  private haveData: boolean = false

  /** Whether the token refresh service is initialized. */
  private tokenService: boolean = false

  private loggedOut: boolean = false

  /** The base URL that auth will redirect to. */
  private get baseUrl (): string {
    return sessionStorage.getItem('BASE_URL')
  }

  /** The registry URL. */
  private get registryUrl (): string {
    // if REGISTRY_URL does not exist this will return 'undefined'. Needs to be null or str
    const configRegistryUrl = sessionStorage.getItem('REGISTRY_URL')
    if (configRegistryUrl) return configRegistryUrl
    return null
  }

  /** True if an error dialog is displayed. */
  private get isErrorDialog (): boolean {
    return (
      this.accountAuthorizationDialog ||
      this.fetchErrorDialog ||
      this.saveErrorDialog
    )
  }

  /** True if Jest is running the code. */
  private get isJestRunning (): boolean {
    return (process.env.JEST_WORKER_ID !== undefined)
  }

  /** Whether user is authenticated. */
  private get isAuthenticated (): boolean {
    return Boolean(sessionStorage.getItem(SessionStorageKeys.KeyCloakToken))
  }

  /** The About text. */
  private get aboutText (): string {
    return process.env.ABOUT_TEXT
  }

  beforeCreate () {
    if (!this.$store.hasModule('account')) {
      this.$store.registerModule('account', AccountOverrideModule)
    }
  }

  /**
   * Called when component is created.
   * NB: User may not be authed yet.
   */
  private created (): void {
    // before unloading this page, if there are changes then prompt user
    window.onbeforeunload = (event) => {
      // add condition once we know what to look for
      if (false) { // eslint-disable-line no-constant-condition
        // cancel closing the page
        event.preventDefault()
        // pop up confirmation dialog
        // NB: custom text is not supported in all browsers
        event.returnValue = 'You have unsaved changes. Are you sure you want to leave?'
      }
    }

    // listen for save error events
    this.$root.$on('save-error-event', async error => {
      // save errors/warnings
      this.saveErrors = error?.response?.data?.errors || []
      this.saveWarnings = error?.response?.data?.warnings || []

      console.log('save error =', error) // eslint-disable-line no-console
      this.saveErrorDialog = true
    })

    // if we are already authenticated then go right to init
    // if not authenticated still continue, as we are allowing unauthenticated users
    if (this.isAuthenticated) this.onProfileReady(true)
  }

  /** Called when component is destroyed. */
  private destroyed (): void {
    // stop listening for custom events
    this.$root.$off('save-error-event')
  }

  /** Called when profile is ready -- we can now init app. */
  @Watch('profileReady')
  private async onProfileReady (val: boolean): Promise<void> {
    //
    // do the one-time things here
    //
    console.log('Profile ready')

    if (val) {
      // start KC token service
      await this.startTokenService()

      // initialize app
      await this.initApp()
    }
  }

  /** Initializes application. Also called for retry. */
  private async initApp (): Promise<void> {
    console.log('initApp')
    //
    // do the repeatable things here
    //

    // reset errors in case of retry
    this.resetFlags()

    // get and store keycloak roles
    try {
      const userProfile: KCUserProfile = getKeyCloakUserProfile()
      this.userName = userProfile.firstName
      if (userProfile && userProfile.roles && userProfile.roles.length > 0) {
        this.keyCloakGroups = userProfile.roles
      } else {
        this.keyCloakGroups = []
      }
    } catch (error) {
      console.log('Keycloak error =', error) // eslint-disable-line no-console
      this.accountAuthorizationDialog = true
      return
    }
    // load Dashboard details
    try {
      await this.fetchDashboardDetails('tabs')
    } catch (error) {
      console.log('FetchDashboard details error =', error) // eslint-disable-line no-console
      this.accountAuthorizationDialog = true
      return
    }

    // finally, let router views know they can load their data
    this.appReady = true
  }

  /** Starts token service that refreshes KC token periodically. */
  private async startTokenService (): Promise<void> {
    // only initialize once
    // don't start during Jest tests as it messes up the test JWT
    if (this.tokenService || this.isJestRunning) return

    try {
      console.info('Starting token refresh service...') // eslint-disable-line no-console
      await KeycloakService.initializeToken()
      this.tokenService = true
    } catch (e) {
      // this happens when the refresh token has expired
      // 1. clear flags and keycloak data
      this.tokenService = false
      this.profileReady = false
      sessionStorage.removeItem(SessionStorageKeys.KeyCloakToken)
      sessionStorage.removeItem(SessionStorageKeys.KeyCloakRefreshToken)
      sessionStorage.removeItem(SessionStorageKeys.KeyCloakIdToken)
      sessionStorage.removeItem(SessionStorageKeys.CurrentAccount)
      // 2. reload app to get new tokens
      console.log(e)
      location.reload()
    }
  }

  private async fetchDashboardDetails (uri: string): Promise<any> {
    const url = sessionStorage.getItem('WTD_API_URL') + '/' + uri
    const headers = {
      Accept: 'application/json',
      ResponseType: 'application/json',
      'Cache-Control': 'no-cache'
    }
    const response = await axios.get(url, { headers }).catch(() => {
      return Promise.reject(new Error('Could not fetch dashboards.json'))
    })
    this.dashboards = response.data.tabs
  }

  private async getEditDashboard () {
    this.haveData = false
    this.appReady = false
    try {
      await this.fetchDashboardDetails('tabs/edit')
    } catch (error) {
      console.log('FetchDashboard details error =', error) // eslint-disable-line no-console
      this.accountAuthorizationDialog = true
      return
    }
    this.appReady = true
    this.haveData = true
  }

  private async updateDashboardDetails (updatedDashboards: DashboardTabIF[]): Promise<any> {
    // get config from environment
    this.haveData = false
    this.appReady = false
    const url = sessionStorage.getItem('WTD_API_URL') + '/tabs'
    const headers = {
      Accept: 'application/json',
      ResponseType: 'application/json',
      'Cache-Control': 'no-cache'
    }

    console.log('updating dashboard to url: ' + url)
    console.log('Dashboards: ' + updatedDashboards[0].tabName)
    this.dashboards = []
    updatedDashboards.forEach(val => this.dashboards.push(Object.assign({}, val)))
    const payload: DashboardPayloadIF = { tabs: updatedDashboards }
    await axios.post(url, payload, { headers }).catch(() => {
      this.saveErrorDialog = true
      return Promise.reject(new Error('Could not post dashboard.json'))
    })

    /**
     * authConfig is a workaround to fix the user settings call as it expects a URL with no trailing slash.
     * This will be removed when a fix is made to sbc-common-components to handle this
     */
    // console.log('tabs response: ' + response)
    // console.log('tabs response: ' + response.data[0].tabName)
    // this.dashboards = response.data
    location.reload()
  }

  /** Resets all error flags/states. */
  private resetFlags (): void {
    this.appReady = false
    this.haveData = false
    this.closeErrorDialogues()
  }

  /** Resets error dialogue flags */
  private closeErrorDialogues (): void {
    this.accountAuthorizationDialog = false
    this.fetchErrorDialog = false
    this.saveErrorDialog = false
    this.saveErrors = []
    this.saveWarnings = []
  }

  /** Fetches current user info and stores it. */
  private async loadUserInfo (): Promise<any> {
    // NB: will throw if API error
    // const response = await this.fetchCurrentUser()
    // const userInfo = response?.data
    // console.log('user info loaded: ' + userInfo)
    // if (userInfo) {
    //   this.setUserInfo(userInfo)
    // } else {
    //   throw new Error('Invalid user info')
    // }
  }
}
</script>

<style lang="scss" scoped>
// place app header on top of dialogs (and therefore still usable)
.app-header {
  z-index: 1000;
}
</style>
