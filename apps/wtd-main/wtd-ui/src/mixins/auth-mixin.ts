// Libraries
import { Component, Vue } from 'vue-property-decorator'
import { axios } from '@/utils'

/**
 * Mixin that provides the integration with the legal api.
 */
@Component({})
export default class AuthMixin extends Vue {
  /**
   * Fetches current user data.
   * @returns a promise to return the user data
   */
  // async fetchCurrentUser (): Promise<any> {
  //   const authUrl = sessionStorage.getItem('AUTH_API_URL')
  //   const config = { baseURL: authUrl }
  // const response = axios.get('users/roblo@idir', config)
  // return response
  // return axios.get(authUrl + '/users', config)
  //   return axios.get('http://localhost:3000/currentUser', config)
  // }
}
