// Setup the global namespace, if not already set.
this['Onyx'] = this['Onyx'] || {};

(function(hv, parent, window, document) {
    'use strict';

    var

        // Namespace.
        self = parent,

        // The documents default location and associated hostname.
        location = window.location,
        hostname = location.hostname,
        protocol = location.protocol,

        devMode = (hostname === 'localhost'),

        appPath = (function() {
            var path = (devMode ? '/onyx' : ''),
                port = (devMode ? '8080' : location.port);
            return {
                'path': path,
                'port': port
            };
        }()),

        apiVersion = (function() {
            return 'v1';
        }()),
        apiPath = (function() {
            var path = (appPath.path + '/api'),
                port = (devMode ? '8080' : location.port);
            return {
                'path': path,
                'port': port
            };
        }()),

        // Dynamically build the base app URL.
        baseUrl = protocol + '//' + hostname + ((appPath.port !== '') ? ':' + appPath.port : ''),
        baseAppUrl = baseUrl + appPath.path,
        baseApiUrl = baseUrl + apiPath.path,
        baseApiVersion = apiVersion;

    // Exports
    self.baseUrl = baseUrl;
    self.baseAppUrl = baseAppUrl;
    self.baseAppPath = appPath.path;
    self.baseApiUrl = baseApiUrl;
    self.baseApiPath = apiPath.path;
    self.baseApiVersion = baseApiVersion;

})(Onyx, Onyx || {}, this, this.document);
