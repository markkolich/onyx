(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.ImageViewer = parent.ImageViewer || {},

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
        };

    init();

})(Onyx.App || {}, this, this.document);
