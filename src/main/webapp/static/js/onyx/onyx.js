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
            var path = (devMode ? '/onyx/' : '/'),
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
            var path = (devMode ? '/onyx/api/' : '/api/'),
                port = (devMode ? '8080' : location.port);
            return {
                'path': path,
                'port': port
            };
        }()),

        // Dynamically build the base app URL.
        baseUrl = protocol + '//' + hostname,
        baseAppUrl = baseUrl + ((appPath.port !== '') ? ':' + appPath.port : '') + appPath.path,
        baseApiUrl = baseUrl + ((apiPath.port !== '') ? ':' + apiPath.port : '') + apiPath.path,
        baseApiVersion = apiVersion,

        // Do we have access to window.console as provided by Firebug or the browser?
        // If not, leave things undefined in which case we'll new up stubs to mimic the logger.
        console = window.console ? window.console : undefined,

        init = (function() {
            return function() {
                // If no console exists, then we define a stub console for
                // each supported logging function.
                if (!console) {
                    var names = ['log', 'debug', 'info', 'warn', 'error',
                        'assert', 'dir', 'dirxml', 'group', 'groupEnd','time',
                        'timeEnd', 'count', 'trace', 'profile', 'profileEnd'];
                    console = {};
                    for (var i = 0, l = names.length; i < l; i++) {
                        console[names[i]] = function() {};
                    }
                }
            };
        }());

    self['baseUrl'] = baseUrl;

    self['baseAppUrl'] = baseAppUrl;
    self['baseAppPath'] = appPath.path;

    self['baseApiUrl'] = baseApiUrl;
    self['baseApiPath'] = apiPath.path;
    self['baseApiVersion'] = baseApiVersion;

    self['console'] = console;

    init();

    return self;

})(Onyx, Onyx || {}, this, this.document);
