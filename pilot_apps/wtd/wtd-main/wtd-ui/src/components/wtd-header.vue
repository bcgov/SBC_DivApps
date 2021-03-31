<template>
  <div>
    <header class="app-header" id="appHeader">
      <v-container class="justify-space-between">
        <a @click="goToHome()" class="brand">
          <picture>
            <source media="(min-width: 601px)"
              srcset="../assets/img/gov_bc_logo_horiz.png">
            <source media="(max-width: 600px)"
              srcset="../assets/img/gov_bc_logo_vert.png">
            <img class="brand__image"
              src="../assets/img/gov_bc_logo_vert.png"
              alt="Government of British Columbia Logo"
              title="Government of British Columbia">
          </picture>
          <span class="brand__title">Service BC</span>
        </a>
        <div v-if="showActions" class="app-header__actions">

          <!-- Product Selector -->
          <sbc-product-selector v-if="showProductSelector" />

          <!-- Login Menu -->
          <v-menu
            fixed
            bottom
            left
            width="330"
            transition="slide-y-transition"
            attach="#appHeader"
            v-if="!isAuthenticated"
          >
            <template v-slot:activator="{ on }">
              <v-btn
                large
                text
                dark
                class="mx-1 pr-2 pl-3"
                aria-label="log in"
                id="loginBtn"
                v-on="on">
                <span>Log in</span>
                <v-icon class="ml-1">mdi-menu-down</v-icon>
              </v-btn>
            </template>
            <v-card>
              <div>
                <v-card-title class="body-2 font-weight-bold">Select login method</v-card-title>
                <v-divider></v-divider>
              </div>
              <v-list
                tile
                dense
              >
                <v-list-item
                  v-for="loginOption in loginOptions"
                  :key="loginOption.idpHint"
                  @click="login(loginOption.idpHint)"
                  class="pr-6"
                >
                  <v-list-item-icon left>
                    <v-icon>{{loginOption.icon}}</v-icon>
                  </v-list-item-icon>
                  <v-list-item-title>{{loginOption.option}}</v-list-item-title>
                </v-list-item>
              </v-list>
            </v-card>
          </v-menu>
          <!-- Account -->
          <v-menu
            bottom
            left
            transition="slide-y-transition"
            attach="#appHeader"
            v-if="isAuthenticated"
          >
            <template v-slot:activator="{ on }">
              <v-btn
                large
                text
                class="user-account-btn"
                aria-label="my account"
                v-on="on"
              >
                <v-avatar
                  tile
                  left
                  color="#4d7094"
                  size="32"
                  class="user-avatar">
                  {{ username.slice(0,1) }}
                </v-avatar>
                <div class="user-info">
                  <div class="user-name" data-test="user-name">{{ username }}</div>
                  <div class="account-name" v-if="!isStaff" data-test="account-name">{{ accountName }}</div>
                </div>
                <v-icon class="ml-1">
                  mdi-menu-down
                </v-icon>
              </v-btn>
            </template>

            <v-card>
              <!-- User Profile -->
              <v-list
                tile
                dense
              >
                <v-list-item two-line>
                  <v-list-item-avatar
                    tile
                    left
                    color="#4d7094"
                    size="36"
                    class="user-avatar white--text">
                    {{ username.slice(0,1) }}
                  </v-list-item-avatar>
                  <v-list-item-content class="user-info">
                    <v-list-item-title class="user-name" data-test="menu-user-name">{{ username }}</v-list-item-title>
                    <v-list-item-subtitle
                      class="account-name"
                      v-if="!isStaff"
                      data-test="menu-account-name"
                      >
                        {{ accountName }}
                      </v-list-item-subtitle>
                  </v-list-item-content>
                </v-list-item>
                <!-- BEGIN: Hide if authentication is IDIR -->
                <v-list-item @click="goToUserProfile()" v-if="isBcscOrBceid">
                  <v-list-item-icon left>
                    <v-icon>mdi-account-outline</v-icon>
                  </v-list-item-icon>
                  <v-list-item-title>Edit Profile</v-list-item-title>
                </v-list-item>
                <!-- END -->
                <v-list-item @click="logout()">
                  <v-list-item-icon left>
                    <v-icon>mdi-logout-variant</v-icon>
                  </v-list-item-icon>
                  <v-list-item-title>Log out</v-list-item-title>
                </v-list-item>
              </v-list>

              <v-divider></v-divider>

              <!-- Account Settings -->
              <v-list
                tile
                dense
                v-if="currentAccount && !isStaff"
              >
                <v-subheader>ACCOUNT SETTINGS</v-subheader>
                <v-list-item @click="goToAccountInfo(currentAccount)">
                  <v-list-item-icon left>
                    <v-icon>mdi-information-outline</v-icon>
                  </v-list-item-icon>
                  <v-list-item-title>Account Info</v-list-item-title>
                </v-list-item>
                <v-list-item @click="goToTeamMembers()">
                  <v-list-item-icon left>
                    <v-icon>mdi-account-group-outline</v-icon>
                  </v-list-item-icon>
                  <v-list-item-title>Team Members</v-list-item-title>
                </v-list-item>
                <v-list-item
                  v-if="showTransactions"
                  @click="goToTransactions()">
                  <v-list-item-icon left>
                    <v-icon>mdi-file-document-outline</v-icon>
                  </v-list-item-icon>
                  <v-list-item-title>Transactions</v-list-item-title>
                </v-list-item>
              </v-list>

              <v-divider></v-divider>

              <!-- Switch Account -->
              <div v-if="!isStaff && !isGovmUser">
                <v-list
                  tile
                  dense
                  v-if="switchableAccounts.length > 1"
                >
                  <v-subheader>SWITCH ACCOUNT</v-subheader>
                  <v-list-item
                    color="primary"
                    :class="{'v-list-item--active' : settings.id === currentAccount.id}"
                    v-for="(settings, id) in switchableAccounts"
                    :key="id"
                    @click="switchAccount(settings, inAuth)"
                  >
                    <v-list-item-icon left>
                      <v-icon v-show="settings.id === currentAccount.id">mdi-check</v-icon>
                    </v-list-item-icon>
                    <v-list-item-title>{{ settings.label }}</v-list-item-title>
                  </v-list-item>
                </v-list>
                <v-divider></v-divider>
              </div>
            </v-card>
          </v-menu>

          <v-btn
            text
            dark
            large
            @click="goToCreateAccount()"
            v-if="!isAuthenticated"
          >
            Create Account
          </v-btn>
        </div>
      </v-container>
    </header>
    <div id="warning-bar">
      <browser-version-alert />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Mixins, Prop, Watch } from 'vue-property-decorator'
import { initialize, LDClient } from 'launchdarkly-js-client-sdk' // eslint-disable-line no-unused-vars
import { Account, IdpHint, LoginSource, Pages, Role } from 'sbc-common-components/src/util/constants'
import ConfigHelper from 'sbc-common-components/src/util/config-helper'
import { mapState, mapActions, mapGetters } from 'vuex'
import { UserSettings } from 'sbc-common-components/src/models/userSettings' // eslint-disable-line no-unused-vars
import Vue from 'vue' // eslint-disable-line no-unused-vars
import NavigationMixin from 'sbc-common-components/src/mixins/navigation-mixin'
import { getModule } from 'vuex-module-decorators'
import AccountModule from 'sbc-common-components/src/store/modules/account'
import AuthModule from 'sbc-common-components/src/store/modules/auth'
import { KCUserProfile } from 'sbc-common-components/src/models/KCUserProfile' // eslint-disable-line no-unused-vars
import keycloakService from 'sbc-common-components/src/services/keycloak.services' // eslint-disable-line no-unused-vars
import LaunchDarklyService from 'sbc-common-components/src/services/launchdarkly.services' // eslint-disable-line no-unused-vars, max-len
import BrowserVersionAlert from 'sbc-common-components/src/components/BrowserVersionAlert.vue'
import SbcProductSelector from 'sbc-common-components/src/components/SbcProductSelector.vue'
import { AccountStatus } from 'sbc-common-components/src/util/enums'

declare module 'vuex' {
  interface Store<S> {
    isModuleRegistered(_: string[]): boolean
  }
}

@Component({
  beforeCreate () {
    this.$store.isModuleRegistered = function (aPath: string[]) {
      let m = (this as any)._modules.root
      return aPath.every((p) => {
        m = m._children[p]
        return m
      })
    }
    if (!this.$store.isModuleRegistered(['account'])) {
      this.$store.registerModule('account', AccountModule)
    }
    if (!this.$store.isModuleRegistered(['auth'])) {
      this.$store.registerModule('auth', AuthModule)
    }
    this.$options.computed = {
      ...(this.$options.computed || {}),
      ...mapState('account', ['currentAccount', 'pendingApprovalCount', 'currentUser']),
      ...mapGetters('account', ['accountName', 'switchableAccounts']),
      ...mapGetters('auth', ['isAuthenticated', 'currentLoginSource'])
    }
    this.$options.methods = {
      ...(this.$options.methods || {}),
      ...mapActions('account', ['loadUserInfo', 'syncAccount', 'syncCurrentAccount', 'syncUserProfile']),
      ...mapActions('auth', ['syncWithSessionStorage'])
    }
  },
  components: {
    SbcProductSelector,
    BrowserVersionAlert
  }
})
export default class WtdHeader extends Mixins(NavigationMixin) {
  private ldClient!: LDClient
  private readonly currentAccount!: UserSettings | null
  private readonly pendingApprovalCount!: number
  private readonly accountName!: string
  private readonly currentLoginSource!: string
  private readonly isAuthenticated!: boolean
  private readonly switchableAccounts!: UserSettings[]
  private readonly loadUserInfo!: () => KCUserProfile
  private readonly syncAccount!: () => Promise<void>
  private readonly syncCurrentAccount!: (userSettings: UserSettings) => Promise<UserSettings>
  private readonly syncUserProfile!: () => Promise<void>
  private readonly syncWithSessionStorage!: () => void
  private readonly currentUser!: any

  @Prop({ default: '' }) redirectOnLoginSuccess!: string;
  @Prop({ default: '' }) redirectOnLoginFail!: string;
  @Prop({ default: '' }) redirectOnLogout!: string;
  @Prop({ default: false }) inAuth!: boolean;
  @Prop({ default: false }) showProductSelector!: boolean;
  @Prop({ default: true }) showActions!: boolean;
  @Prop({ default: '' }) username!: string;

  private readonly loginOptions = [
    {
      idpHint: IdpHint.BCSC,
      option: 'BC Services Card',
      icon: 'mdi-account-card-details-outline'
    },
    {
      idpHint: IdpHint.BCEID,
      option: 'BCeID',
      icon: 'mdi-two-factor-authentication'
    },
    {
      idpHint: IdpHint.IDIR,
      option: 'IDIR',
      icon: 'mdi-account-group-outline'
    }
  ]

  get showTransactions (): boolean {
    return this.currentAccount?.accountType === Account.PREMIUM
  }

  // only for internal staff who belongs to bcreg
  get isStaff (): boolean {
    return this.currentUser && this.currentUser.roles.includes(Role.Staff)
  }

  // only for GOVN type users
  get isGovmUser (): boolean {
    return this.currentUser && this.currentUser.roles.includes(Role.GOVMAccountUser)
  }

  get isBceid (): boolean {
    return this.currentLoginSource === LoginSource.BCEID
  }

  get isBcscOrBceid (): boolean {
    return [LoginSource.BCSC.valueOf(), LoginSource.BCEID.valueOf()].indexOf(this.currentLoginSource) >= 0
  }

  private async mounted () {
    getModule(AccountModule, this.$store)
    getModule(AuthModule, this.$store)
    this.syncWithSessionStorage()
    if (this.isAuthenticated) {
      await this.loadUserInfo()
      await this.syncAccount()
      await this.updateProfile()
      // checking for account status
      await this.checkAccountStatus()
    }
  }

  @Watch('isAuthenticated')
  private async onisAuthenticated (isAuthenitcated: string, oldVal: string) {
    if (isAuthenitcated) {
      await this.updateProfile()
    }
  }

  private async updateProfile () {
    if (this.isBceid) {
      await this.syncUserProfile()
    }
  }

  private goToHome () {
    this.redirectToPath(this.inAuth, Pages.HOME)
  }

  private goToUserProfile () {
    this.redirectToPath(this.inAuth, Pages.USER_PROFILE)
  }

  private goToCreateAccount () {
    this.redirectToPath(this.inAuth, Pages.CHOOSE_AUTH_METHOD)
  }

  private goToCreateBCSCAccount () {
    this.redirectToPath(this.inAuth, Pages.CREATE_ACCOUNT)
  }

  private async goToAccountInfo (settings: UserSettings) {
    if (!this.currentAccount || !settings) {
      return
    }
    await this.syncCurrentAccount(settings)
    this.redirectToPath(this.inAuth, `${Pages.ACCOUNT}/${this.currentAccount.id}/${Pages.SETTINGS}/account-info`)
  }

  private goToTeamMembers () {
    if (!this.currentAccount) {
      return
    }
    this.redirectToPath(this.inAuth, `${Pages.ACCOUNT}/${this.currentAccount.id}/${Pages.SETTINGS}/team-members`)
  }

  private goToTransactions () {
    if (!this.currentAccount) {
      return
    }
    this.redirectToPath(this.inAuth, `${Pages.ACCOUNT}/${this.currentAccount.id}/${Pages.SETTINGS}/transactions`)
  }

  private checkAccountStatus () {
    // redirect if accoutn status is suspended
    if ([AccountStatus.NSF_SUSPENDED, AccountStatus.SUSPENDED].some(status => status === this.currentAccount?.accountStatus)) { // eslint-disable-line max-len
      this.redirectToPath(this.inAuth, `${Pages.ACCOUNT_FREEZ}`)
    } else if (this.currentAccount?.accountStatus === AccountStatus.PENDING_STAFF_REVIEW) {
      this.redirectToPath(this.inAuth, `${Pages.PENDING_APPROVAL}/${this.accountName}/true`)
    }
  }

  private async switchAccount (settings: UserSettings, inAuth?: boolean) {
    this.$emit('account-switch-started')
    if (this.$route.params.orgId) {
      // If route includes a URL param for account, we need to refresh with the new account id
      this.$router.push({ name: this.$route.name, params: { orgId: settings.id } })
    }
    await this.syncCurrentAccount(settings)
    this.$emit('account-switch-completed')

    if (!inAuth) {
      window.location.assign(`${ConfigHelper.getAuthContextPath()}/${Pages.HOME}`)
    }
  }

  logout () {
    if (this.redirectOnLogout) {
      const url = encodeURIComponent(this.redirectOnLogout)
      window.location.assign(`${this.getContextPath()}signout/${url}`)
    } else {
      window.location.assign(`${this.getContextPath()}signout`)
    }
  }

  login (idpHint) {
    if (this.redirectOnLoginSuccess) {
      let url = encodeURIComponent(this.redirectOnLoginSuccess)
      url += this.redirectOnLoginFail ? `/${encodeURIComponent(this.redirectOnLoginFail)}` : ''
      window.location.assign(`${this.getContextPath()}signin/${idpHint}/${url}`)
    } else {
      window.location.assign(`${this.getContextPath()}signin/${idpHint}`)
    }
  }

  private getContextPath (): string {
    let baseUrl = (this.$router && (this.$router as any)['history'] && (this.$router as any)['history'].base) || '' // eslint-disable-line dot-notation, max-len
    baseUrl += (baseUrl.length && baseUrl[baseUrl.length - 1] !== '/') ? '/' : ''
    return baseUrl
  }
}
</script>

<style lang="scss" scoped>
@import "sbc-common-components/src/assets/scss/theme.scss";

$app-header-font-color: #ffffff;

.app-header {
  height: 70px;
  color: $app-header-font-color;
  border-bottom: 2px solid $BCgovGold5;
  background-color: $BCgovBlue5;

  .container {
    display: flex;
    align-items: center;
    height: 100%;
    padding-top: 0;
    padding-bottom: 0;
  }
}

.brand {
  display: flex;
  align-items: center;
  padding-right: 1rem;
  text-decoration: none;
  color: inherit;
}

.brand__image {
  display: block;
  margin-right: 1.25rem;
  max-height: 70px;
}

.brand__title {
  letter-spacing: -0.03rem;
  font-size: 1.125rem;
  font-weight: 700;
  color: inherit;
}

.user-avatar {
  border-radius: 0.15rem;
  font-size: 1.1875rem;
  font-weight: 700;
}

@media (max-width: 900px) {
  .brand__image {
    margin-right: 0.75rem;
    margin-left: -0.15rem;
  }

  .brand__title {
    font-size: 1rem;
    line-height: 1.25rem;
  }

  .brand__title--wrap {
    display: block;
  }
}

.v-btn.user-account-btn {
  padding-right: 0.5rem !important;
  padding-left: 0.5rem !important;
  text-align: left;
  color: $app-header-font-color;
  letter-spacing: 0.02rem;
  font-size: 0.8rem;

  .user-avatar {
    margin-right: 0.75rem;
  }

  .user-name {
    line-height: 1.125rem;
    font-size: 0.875rem;
  }

  .account-name {
    margin-bottom: 0.01rem;
    font-size: 0.7rem;
    opacity: 0.75;
  }
}

.v-btn.notifications-btn {
  min-width: 3.142rem !important;
  color: $app-header-font-color;

  .v-badge {
    margin-right: 0.25rem;
  }
}

.v-list {
  border-radius: 0;

  .v-list-item__title,
  .v-list-item__subtitle {
    line-height: normal !important;
  }
}

.v-list .v-list-item__title.user-name,
.user-name {
  font-size: 0.875rem;
  font-weight: 400;
}

.v-list .v-list-item__subtitle.account-name,
.account-name {
  font-size: 0.75rem;
}

.v-list--dense .v-subheader,
.v-list-item {
  padding-right: 1.25rem;
  padding-left: 1.25rem;
}

.v-list--dense .v-subheader,
.v-list--dense .v-list-item__title,
.v-list--dense .v-list-item__subtitle {
  font-size: 0.875rem !important;
}

.v-subheader {
  color: $gray9 !important;
  font-weight: 700;
}

.menu-header {
  display: none;
}

@media (max-width: 1263px) {
  .v-btn.mobile-icon-only {
    min-width: 3rem !important;
    width: 3rem;

    .v-icon + span,
    span + .v-icon {
      display: none;
    }

    .v-icon {
      margin-right: 0;
    }
  }

  .v-btn.user-account-btn {
    min-width: auto !important;
    font-size: 0.8rem;

    .user-avatar {
      margin-right: 0;
    }

    .user-info,
    .v-icon {
      display: none;
    }
  }

  .v-btn.login-btn {
    .v-icon + span,
    span + .v-icon {
      display: none;
    }
  }

  .menu-header {
    display: block;
  }
}

@media (min-width: 1360px) {
  .v-menu__content {
    max-width: 22rem;
  }
}
</style>
