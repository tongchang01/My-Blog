export default {
  qqLogin: {
    QQ_APP_ID: '101999415',
    QQ_REDIRECT_URI: 'https://www.linhaojun.top/oauth/login/qq'
  },
  routes: [
    {
      name: 'Home',
      path: '/',
      i18n: {
        key: 'home'
      },
      children: []
    },
    {
      name: 'Talks',
      path: '/talks',
      i18n: {
        key: 'talks'
      },
      children: []
    },
    {
      name: 'About',
      path: '/about',
      i18n: {
        key: 'about'
      },
      children: []
    },
    {
      name: 'Archives',
      path: '/archives',
      i18n: {
        key: 'archives'
      },
      children: []
    },
    {
      name: 'Tags',
      path: '/tags',
      i18n: {
        key: 'tags'
      },
      children: []
    },
    {
      name: 'Message',
      path: '/message',
      i18n: {
        key: 'message'
      },
      children: []
    },
    {
      name: 'Friends',
      path: '/friends',
      i18n: {
        key: 'friends'
      },
      children: []
    }
  ]
}
