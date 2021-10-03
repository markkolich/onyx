(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.Previewer = parent.Previewer || {},

        data = {
            $contentDiv: $('#content')
        },

        init = function() {
            // Show image resources in a Featherlight lightbox preview.
            data.$contentDiv.find('a[data-resource-type="FILE"]').filter(function() {
                return this.href.match(/\.(gif|jpe?g|tiff|png|webp)$/i);
            }).featherlight({
                type: 'image',
                targetAttr: 'href',
                // No close icon.
                closeIcon: '',
                openSpeed: 100,
                closeSpeed: 100
            });

            // Show video and PDF resources in a Featherlight lightbox preview.
            data.$contentDiv.find('a[data-resource-type="FILE"]').filter(function() {
                return this.href.match(/\.(mov|mp4|mp3|pdf)$/i);
            }).featherlight({
                type: 'iframe',
                targetAttr: 'href',
                // https://github.com/noelboss/featherlight/issues/365#issuecomment-551329397
                iframeStyle: 'width:95vw;max-width:90vw;height:90vh;border-radius:0',
                // No close icon.
                closeIcon: '',
                openSpeed: 100,
                closeSpeed: 100,
                afterOpen: function() {
                    // Reach into the <iframe> and force the browser default video player
                    // to 100% width & 100% height so the player fills the lightbox.
                    $('iframe[class="featherlight-inner"]').contents().find('video')
                        .css('width', '100%')
                        .css('height', '100%');
                }
            });
        };

    init();

})(Onyx.App || {}, this, this.document);
