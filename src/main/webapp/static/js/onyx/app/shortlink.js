(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.ShortLink = parent.ShortLink || {},

        data = {
            $contentDiv: $('#content')
        },

        getShortLink = (function() {

            const
                $modal = $('#get-shortlink-modal'),

                showModal = () => {
                    // Clear any shortlink that may already be in the input field.
                    $modal.find('input[data-shortlink="link"]').val('');

                    $modal.one('shown.bs.modal', () => {
                        const path = $('body[data-path]').data('path');

                        $.ajax({
                            type: 'POST',
                            url: `${parent.baseApiUrl}/v1/shortlink${path}`,
                            contentType: 'application/json',
                            success: (res) => {
                                const shortLinkUrl = res.shortLinkUrl;

                                // Convenience
                                $modal.find('input[data-shortlink="link"]')
                                    .val(shortLinkUrl)
                                    .select();
                            }
                        });
                    });

                    $modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        init = () => {
            data.$contentDiv.find('[data-action="get-shortlink"]').on('click', (e) => {
                e.preventDefault();

                getShortLink.showModal();
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
