// Core Libraries
import 'core-js/stable' // to polyfill ECMAScript features
import 'regenerator-runtime/runtime' // to use transpiled generator functions

import Vue from 'vue'
import Vuetify from 'vuetify/lib'
import Vuelidate from 'vuelidate'
import VueCompositionApi from '@vue/composition-api'
import { getVueRouter } from '@/router'
import { getVuexStore } from '@/store'
// import Affix from 'vue-affix'

// Styles
// NB: order matters - do not change
import '@mdi/font/css/materialdesignicons.min.css' // ensure you are using css-loader
import '@/assets/styles/base.scss'
import '@/assets/styles/layout.scss'
import '@/assets/styles/overrides.scss'

import App from './App.vue'

// Helpers
import { fetchConfig, initLdClient } from '@/utils'
import KeycloakService from 'sbc-common-components/src/services/keycloak.services'

// get rid of "element implicitly has an 'any' type..."
declare const window: any

Vue.config.productionTip = false

Vue.use(VueCompositionApi)
Vue.use(Vuetify)
// Vue.use(Affix)
Vue.use(Vuelidate)

// main code
async function start () {
  // fetch config from environment and API
  // must come first as inits below depend on config
  await fetchConfig()

  // initialize Launch Darkly
  // if (window.ldClientId) {
  //   await initLdClient()
  // }

  // configure KeyCloak Service
  console.info('Starting Keycloak service...') // eslint-disable-line no-console
  await KeycloakService.setKeycloakConfigUrl(sessionStorage.getItem('KEYCLOAK_CONFIG_PATH'))

  // start Vue application
  console.info('Starting app...') // eslint-disable-line no-console
  new Vue({
    vuetify: new Vuetify({
      iconfont: 'mdi',
      theme: {
        themes: {
          light: {
            primary: '#1669bb', // same as $$primary-blue
            'app-dk-blue': '#38598a',
            error: '#d3272c',
            success: '#1a9031'
          }
        }
      }
    }),
    router: getVueRouter(),
    store: getVuexStore(),
    render: h => h(App)
  }).$mount('#app')
}

// execution and error handling
start().catch(error => {
  console.error(error) // eslint-disable-line no-console
  alert('There was an error starting this page. (See console for details.)\n' +
    'Please try again later.')
})
