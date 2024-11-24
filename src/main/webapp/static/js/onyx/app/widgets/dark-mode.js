(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.DarkMode = parent.DarkMode || {},

        data = {
            $body: $('body'),
            $toggle: $('#darkModeToggle'),
            $icon: $('#darkModeToggle i')
        },

        localStorageKey = 'onyx.dark-mode',

        enable = function() {
            data.$body.addClass('dark-mode');
            localStorage.setItem(localStorageKey, 'true');
            data.$icon.removeClass('fa-sun').addClass('fa-moon');
        },

        disable = function() {
            data.$body.removeClass('dark-mode');
            localStorage.setItem(localStorageKey, 'false');
            data.$icon.removeClass('fa-moon').addClass('fa-sun');
        },

        toggle = function() {
            if (data.$body.hasClass('dark-mode')) {
                disable();
            } else {
                enable();
            }
        },

        init = function() {
            // Disable dark mode if explicitly disabled (default is enabled)
            var savedPreference = localStorage.getItem(localStorageKey);
            if (savedPreference === 'false') {
                disable();
            }

            // Bind toggle button click handler
            data.$toggle.on('click', function(e) {
                e.preventDefault();
                toggle();
            });
        };

    // Exports
    self.toggle = toggle;

    init();

})(Onyx.App || {}, this, this.document);
