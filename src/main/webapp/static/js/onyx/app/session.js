(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.Session = parent.Session || {},

        data = {
            timer: null,
            errorCount: 0
        },

        refresh = function() {
            $.ajax({
                type: 'GET',
                url: Onyx.baseAppUrl + '/keepalive',
                success: function(res, status, xhr) {
                    // Reset the error counter, on success.
                    data.errorCount = 0;

                    start();
                },
                error: function(xhr, status, error) {
                    if (data.errorCount >= 5) {
                        stop();
                    } else {
                        data.errorCount++;
                        start();
                    }
                }
            });
        },

        start = function() {
            data.timer = setTimeout(refresh, 1.8e6); // 30-minutes
        },

        stop = function() {
            data.timer && clearTimeout(data.timer);
            data.timer = null;
        },

        init = function() {
            start();
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
