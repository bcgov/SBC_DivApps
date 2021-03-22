import { Dashboard, Signin, Signout } from '@/views'
import { RouteNames } from '@/enums'

export const routes = [
  {
    // router.beforeEach() routes here:
    path: '/signin?redirect',
    name: 'redirect',
    component: Dashboard,
    props: true,
    meta: {
      requiresAuth: false
    }
  },
  {
    // router.beforeEach() routes here:
    path: '/signin',
    name: RouteNames.SIGN_IN,
    component: Signin,
    props: true,
    meta: {
      requiresAuth: false
    }
  },
  {
    // SbcHeader.logout() redirects here:
    path: '/signout/:redirectUrl',
    name: RouteNames.SIGN_OUT,
    component: Signout,
    props: true,
    meta: {
      requiresAuth: false
    }
  },
  {
    path: '/dashboard',
    name: RouteNames.DASHBOARD,
    component: Dashboard,
    meta: {
      // rlo - changed this to false
      requiresAuth: true
    }
  },
  {
    path: '*',
    redirect: '/dashboard'
  }
]
