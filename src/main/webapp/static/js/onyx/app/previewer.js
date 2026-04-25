(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Previewer = parent.Previewer || {},

        data = {
            $contentDiv: $('#content')
        },

        // Suspend/resume keyboard shortcuts via keyboard.js exports. MFP uses
        // triggerHandler() internally so its events do not bubble to document —
        // the only reliable hook is the callbacks option passed to magnificPopup().
        onMfpOpen = () => {
            parent.Keyboard && parent.Keyboard.suspend();
        },

        onMfpAfterClose = () => {
            parent.Keyboard && parent.Keyboard.resume();
        },

        init = () => {
            // Collect all previewable image resources
            const $imageLinks = data.$contentDiv.find('a[data-resource-type="FILE"]').filter(function() {
                return this.href.match(/\.(gif|jpe?g|tiff|png|webp)$/i);
            });
            // Show image resources in a Magnific Popup lightbox with gallery navigation
            $imageLinks.magnificPopup({
                type: 'image',
                gallery: {
                    enabled: true,
                    navigateByImgClick: true,
                    preload: [0, 1],
                    tCounter: '',
                    arrowMarkup: ''
                },
                image: {
                    titleSrc: (item) => item.el.attr('title') || ''
                },
                callbacks: {
                    open: onMfpOpen,
                    afterClose: onMfpAfterClose
                },
                showCloseBtn: false,
                closeBtnInside: true,
                closeOnContentClick: false,
                mainClass: 'mfp-fade',
                removalDelay: 100
            });

            // Collect all previewable media resources
            const $mediaLinks = data.$contentDiv.find('a[data-resource-type="FILE"]').filter(function() {
                return this.href.match(/\.(mp4|mp3)$/i);
            });
            // Show media in a Magnific Popup iframe with gallery navigation
            $mediaLinks.magnificPopup({
                type: 'iframe',
                gallery: {
                    enabled: true,
                    preload: [0, 1],
                    tCounter: '',
                    arrowMarkup: ''
                },
                iframe: {
                    patterns: {
                        // Default patterns for YouTube, Vimeo, etc. are built-in
                        // Add custom pattern for direct video/audio/pdf files
                        directFile: {
                            index: '',
                            src: '%id%'
                        }
                    },
                    srcAction: 'iframe_src'
                },
                callbacks: {
                    open: () => {
                        onMfpOpen();

                        // Reach into the <iframe> and force the browser default video player
                        // to 100% width & 100% height so the player fills the lightbox.
                        const $iframe = $('.mfp-iframe');
                        //$iframe.attr('allow', 'autoplay; fullscreen');
                        //$iframe.attr('allowfullscreen', '');
                        $iframe.on('load', function() {
                            const $video = $(this).contents().find('video')
                                .css('width', '100%')
                                .css('height', '100%');

                            // Attempt to autoplay the <video>
                            if ($video.length) {
                                $video[0].play().catch(() => {
                                    // Autoplay was prevented, user must click play manually
                                });
                            }
                        });
                    },
                    afterClose: onMfpAfterClose
                },
                showCloseBtn: false,
                closeBtnInside: true,
                closeOnContentClick: false,
                mainClass: 'mfp-fade',
                removalDelay: 100
            });
        };

    init();

})(Onyx.App || {}, this, this.document);
