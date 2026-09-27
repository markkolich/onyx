(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Screencaps = parent.Screencaps || {},

        data = {
            $contentDiv: $('#content')
        },

        // Wait this long after mouseenter before looking anything up, so scrolling the cursor
        // across a long file list doesn't fire a request per row passed over.
        HOVER_DELAY_MS = 350,

        // Bootstrap's default popover template, with a class added to the root element so the
        // sizing rules in sb-admin-2.css only ever target this one kind of popover, never
        // popovers added elsewhere in the app later.
        POPOVER_TEMPLATE = '<div class="popover screencaps-popover" role="tooltip">' +
            '<div class="arrow"></div><div class="popover-body"></div></div>',

        buildImageUrl = (resourcePath, key) =>
            `${parent.baseApiUrl}/v1/metadata-download${resourcePath}?key=${encodeURIComponent(key)}`,

        // Initializes (but does not show) the popover's content - separate from showing it so a
        // later hover of the same link can just re-show what's already set up, without
        // re-initializing or re-fetching anything.
        initPreview = ($link, resourcePath, key) => {
            const imgUrl = buildImageUrl(resourcePath, key);

            $link.popover({
                trigger: 'manual',
                html: true,
                placement: 'auto',
                container: 'body',
                template: POPOVER_TEMPLATE,
                content: `<img src="${imgUrl}" class="screencaps-img">`
            });
        },

        // Looks up the latest screencaps key exactly once per link. callback(found) fires once
        // the answer is known, whether or not the mouse is still hovering by that point - it's
        // up to the caller to decide whether "found" still means "show it right now". The API
        // returns items sorted oldest-to-newest by last-modified time, so the latest is always
        // the last element - no client-side sorting needed.
        lookupPreview = ($link, resourcePath, callback) => {
            $.get(`${parent.baseApiUrl}/v1/metadata${resourcePath}`, {type: 'screencaps'})
                .done((response) => {
                    const items = (response && response.items) || [];
                    if (items.length === 0) {
                        callback(false);
                        return;
                    }
                    initPreview($link, resourcePath, items[items.length - 1].key);
                    callback(true);
                })
                .fail(() => callback(false));
        },

        bindHoverPreview = ($link, resourcePath) => {
            let hoverTimer = null;
            let hovering = false;

            // Set once the lookup completes, whether or not a preview was found - checked/
            // hasPreview together mean later hovers of this same link can just re-show the
            // already-initialized popover (or do nothing, if none exists) instead of re-issuing
            // the list call.
            let checked = false;
            let hasPreview = false;

            $link.on('mouseenter', () => {
                hovering = true;

                if (checked) {
                    if (hasPreview) {
                        $link.popover('show');
                    }
                    return;
                }

                hoverTimer = window.setTimeout(() => {
                    lookupPreview($link, resourcePath, (found) => {
                        checked = true;
                        hasPreview = found;
                        // Only pop it open now if the mouse hasn't already left - a slow lookup
                        // shouldn't cause a popover to appear after the fact.
                        if (found && hovering) {
                            $link.popover('show');
                        }
                    });
                }, HOVER_DELAY_MS);
            });

            $link.on('mouseleave', () => {
                hovering = false;

                if (hoverTimer) {
                    window.clearTimeout(hoverTimer);
                    hoverTimer = null;
                }
                // Only call popover('hide') if popover() was actually initialized on this link -
                // calling it beforehand (a hover shorter than HOVER_DELAY_MS, or before the first
                // lookup ever resolves) throws.
                if ($link.data('bs.popover')) {
                    $link.popover('hide');
                }
            });
        },

        init = () => {
            // Screencaps only apply to video.
            const $videoLinks = data.$contentDiv.find('a[data-resource-type="FILE"]').filter(function() {
                return this.href.match(/\.(mp4|mov|mpg|mpeg|avi|webm|wmv|m4v|mkv)$/i);
            });

            $videoLinks.each(function() {
                const $link = $(this);
                const $row = $link.closest('tr[data-resource]');
                if ($row.length === 0) {
                    return;
                }
                bindHoverPreview($link, $row.data('resource'));
            });
        };

    init();

})(Onyx.App || {}, this, this.document);
