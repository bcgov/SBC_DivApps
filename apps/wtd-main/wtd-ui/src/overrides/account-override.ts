import { Action, Module } from 'vuex-module-decorators'
import AccountModule from 'sbc-common-components/src/store/modules/account'

@Module({
  name: 'account',
  namespaced: true
})
export default class AccountOverrideModule extends AccountModule {
  @Action({ rawError: true })
  public async syncAccount () {
    // do nothing
  }

  @Action({ rawError: true })
  public async updateUserProfile () {
    // do nothing
  }

  @Action({ rawError: true })
  public async getCurrentUserProfile (isAuth: boolean = false) {
    try {
      const userProfile = null
      return userProfile
    } catch (error) {
      console.error('Error: ', error?.response)
    }
  }
}
