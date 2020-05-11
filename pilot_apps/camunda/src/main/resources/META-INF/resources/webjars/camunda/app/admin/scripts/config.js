if (window.location.href.includes("http://test-") || window.location.href.includes("https://test-")) {
  document.querySelector("[cam-widget-header]").style.backgroundColor = "yellow";
} else if (window.location.href.includes("http://dev-") || window.location.href.includes("https://dev-")) {
  document.querySelector("[cam-widget-header]").style.backgroundColor = "green";
}

window.camAdminConf = {
  customScripts: {
    // AngularJS module names
    ngDeps: ['custom-logout'],
    // RequireJS configuration for a complete configuration documentation see:
    // http://requirejs.org/docs/api.html#config
    deps: ['jquery', 'custom-logout'],
    paths: {
      'custom-logout': 'custom/logout'
    }
  },

  app: {
    name: 'Service Flow',
    vendor: ' '
  }
};