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

        // Grace period between the cursor leaving the link and the popover closing, so the
        // mouse can cross the gap into the popover without it vanishing en route. Entering
        // the popover inside this window cancels the close - without it the sheet can't be
        // scrolled at all, since reaching for it is what would dismiss it.
        HIDE_GRACE_MS = 150,

        // Bootstrap's default popover template, with a class added to the root element so the
        // sizing rules in sb-admin-2.css only ever target this one kind of popover, never
        // popovers added elsewhere in the app later.
        POPOVER_TEMPLATE = '<div class="popover screencaps-popover" role="tooltip">' +
            '<div class="arrow"></div><div class="popover-body"></div></div>',

        // The link whose popover is open, plus any pending close. Module scope rather than a
        // per-link closure because the bridge handlers are bound once on document and have to
        // act on whichever popover happens to be open.
        state = {
            $activeLink: null,
            hideTimer: null
        },

        buildImageUrl = (resourcePath, key) =>
            `${parent.baseApiUrl}/v1/metadata-download${resourcePath}?key=${encodeURIComponent(key)}`,

        // The sheet renders at its native size inside a fixed-size scrolling window, rather
        // than being scaled down to fit the popover. Scaling to fit is what made frames
        // unreadable on long videos: tools/screencaps.sh sizes the tile grid to the frame
        // count (ceil(sqrt(n)), up to 400 tiles), so a long video's sheet reaches roughly
        // 6900px across and shrinking that into a popover left each tile under 90px wide.
        buildContent = (imgUrl) =>
            '<div class="screencaps-scroller">' +
                `<img class="screencaps-img" src="${imgUrl}" alt="">` +
            '</div>',

        cancelHide = () => {
            if (state.hideTimer) {
                window.clearTimeout(state.hideTimer);
                state.hideTimer = null;
            }
        },

        hideActive = () => {
            cancelHide();
            // Only call popover('hide') if popover() was actually initialized on this link -
            // calling it beforehand throws.
            if (state.$activeLink && state.$activeLink.data('bs.popover')) {
                state.$activeLink.popover('hide');
            }
            state.$activeLink = null;
        },

        scheduleHide = () => {
            cancelHide();
            state.hideTimer = window.setTimeout(hideActive, HIDE_GRACE_MS);
        },

        showFor = ($link) => {
            // Bootstrap 4.3.1's show() has no already-shown guard: it mints a fresh tip id and
            // re-runs setContent(), which rebuilds the <img> - refetching the sheet and losing
            // wherever it had been scrolled to. Moving the cursor off the popover and back onto
            // the link would otherwise do exactly that, so bail if this link's popover is up.
            // Bootstrap sets aria-describedby on the trigger in show() and removes it in hide(),
            // which makes it a reliable "currently shown" flag.
            if ($link.attr('aria-describedby')) {
                state.$activeLink = $link;
                return;
            }

            state.$activeLink = $link;
            $link.popover('show');
        },

        // Initializes (but does not show) the popover's content - separate from showing it so a
        // later hover of the same link can just re-show what's already set up, without
        // re-initializing or re-fetching anything.
        initPreview = ($link, resourcePath, key) => {
            $link.popover({
                trigger: 'manual',
                html: true,
                // Pinned to the right. 'auto' let Popper pick whichever side had the most room,
                // which on a wide preview is usually top or bottom - landing the popover over the
                // row it describes. fallbackPlacement with a single entry also stops Popper
                // flipping away from the right when space there is tight; Bootstrap 4.3.1 has no
                // popperConfig option, so these two are the only placement levers it exposes.
                placement: 'right',
                fallbackPlacement: ['right'],
                container: 'body',
                template: POPOVER_TEMPLATE,
                content: buildContent(buildImageUrl(resourcePath, key))
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
                cancelHide();

                // Another row's popover can still be open if the cursor arrived here directly
                // from it, inside the grace window.
                if (state.$activeLink && state.$activeLink[0] !== $link[0]) {
                    hideActive();
                }

                if (checked) {
                    if (hasPreview) {
                        showFor($link);
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
                            showFor($link);
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
                if ($link.data('bs.popover')) {
                    scheduleHide();
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

            // Bound once on document, delegated: Bootstrap builds and tears down the tip element
            // on every show/hide, so there is nothing stable to bind per link. Keeping the
            // popover alive while the cursor is inside it is what makes the sheet scrollable.
            $(document)
                .on('mouseenter', '.screencaps-popover', cancelHide)
                .on('mouseleave', '.screencaps-popover', scheduleHide);
        };

    init();

})(Onyx.App || {}, this, this.document);
