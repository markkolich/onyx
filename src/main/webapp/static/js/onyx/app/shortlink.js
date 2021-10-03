(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.ShortLink = parent.ShortLink || {},

        data = {
            $contentDiv: $('#content')
        },

        getShortLink = (function() {

            var
                modal = $('#get-shortlink-modal'),

                showModal = function() {
                    // Clear any shortlink that may already be in the input field.
                    modal.find('input[data-shortlink="link"]').val('');

                    modal.on('shown.bs.modal', function() {
                        var path = $('body[data-path]').data('path');

                        $.ajax({
                            type: 'POST',
                            url: parent.baseApiUrl + '/v1/shortlink' + path,
                            contentType: 'application/json',
                            success: function(res, status, xhr) {
                                var shortLinkUrl = res.shortLinkUrl;

                                // Convenience
                                modal.find('input[data-shortlink="link"]')
                                    .val(shortLinkUrl)
                                    .select();
                            }
                        });
                    });

                    modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        init = function() {
            data.$contentDiv.find('[data-action="get-shortlink"]').unbind().click(function(e) {
                getShortLink.showModal();

                e.preventDefault();
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
