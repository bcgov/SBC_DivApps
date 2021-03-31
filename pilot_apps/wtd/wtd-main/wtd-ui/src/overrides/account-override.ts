import { Action, Module } from 'vuex-module-decorators'
import { CurrentUserIF } from '@/interfaces'
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
      const userProfile: CurrentUserIF = {
        userTerms: {
          isTermsOfUseAccepted: true
        }
      }
      // const userProfile = null //currentUser?.userTerms?.isTermsOfUseAccepted
      return userProfile
    } catch (error) {
      console.error('Error: ', error?.response)
    }
  }
}
