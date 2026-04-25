(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.App = parent.App || {},

        init = () => {
            // Auto-focus input elements in dropdowns on shown:
            // https://stackoverflow.com/a/52186879
            $('.dropdown').one('shown.bs.dropdown', () => {
                $('.dropdown-menu input').focus();
            });
        };

    // Exports
    self.baseAppUrl = parent.baseAppUrl;
    self.baseApiUrl = parent.baseApiUrl;

    init();

})(Onyx || {}, this, this.document);
