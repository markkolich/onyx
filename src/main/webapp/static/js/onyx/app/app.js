(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.App = parent.App || {},

        init = function() {
            // Auto-focus input elements in dropdowns on shown:
            // https://stackoverflow.com/a/52186879
            $('.dropdown').on('shown.bs.dropdown', function(e) {
                $('.dropdown-menu input').focus();
            });
        };

    // Exports
    self.baseAppUrl = parent.baseAppUrl;
    self.baseApiUrl = parent.baseApiUrl;

    init();

})(Onyx || {}, this, this.document);
