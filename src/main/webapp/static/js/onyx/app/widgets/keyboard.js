(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Keyboard = parent.Keyboard || {},

        data = {
            selectedIndex: -1,
            kpListener: null,
            suspended: 0
        },

        suspend = () => {
            if (data.suspended === 0) {
                data.kpListener.stop_listening();
            }
            data.suspended++;
        },

        resume = () => {
            data.suspended = Math.max(0, data.suspended - 1);
            if (data.suspended === 0) {
                data.kpListener.listen();
            }
        },

        getRows = () => $('tr[data-resource]').toArray(),

        selectRow = (index) => {
            const rows = getRows();
            if (!rows.length) {
                return;
            }
            data.selectedIndex = Math.max(0, Math.min(index, rows.length - 1));
            $('tr[data-resource]').removeClass('keyboard-selected');
            rows[data.selectedIndex].classList.add('keyboard-selected');
            rows[data.selectedIndex].scrollIntoView({ block: 'nearest' });
        },

        init = () => {
            data.kpListener = new window.keypress.Listener();

            // / — focus search
            data.kpListener.register_combo({
                keys: '/',
                on_keydown: (e) => {
                    if (e.shiftKey) {
                        return;
                    }
                    $('#searchDropdown').dropdown('toggle');
                    setTimeout(() => {
                        $('.navbar-search input[name="query"]').focus();
                    }, 100);
                }
            });

            // Escape — close any open modal
            data.kpListener.register_combo({
                keys: 'escape',
                on_keydown: () => {
                    $('.modal.show').modal('hide');
                }
            });

            // Enter — open selected row
            data.kpListener.register_combo({
                keys: 'enter',
                on_keydown: () => {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    const $link = $(getRows()[data.selectedIndex]).find('a[data-resource-type]').last();
                    if ($link.length) {
                        $link.get(0).click();
                    }
                }
            });

            // d — open details view for selected row
            /*data.kpListener.register_combo({
                keys: 'd',
                on_keydown: () => {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    // Intentionally find('a') and not find('a[data-resource-type]') here:
                    const $link = $(getRows()[data.selectedIndex]).find('a').first();
                    if ($link.length) {
                        $link.get(0).click();
                    }
                }
            });*/

            // u — upload file
            data.kpListener.register_combo({
                keys: 'u',
                on_keydown: () => {
                    $('[data-action="upload-file"]').first()
                        .trigger('click');
                }
            });

            // n — new directory
            data.kpListener.register_combo({
                keys: 'n',
                on_keydown: () => {
                    $('[data-action="create-directory"]').first()
                        .trigger('click');
                }
            });

            // v — toggle visibility of selected row
            /*data.kpListener.register_combo({
                keys: 'v',
                on_keydown: () => {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    const $row = $(getRows()[data.selectedIndex]);
                    $row
                        .find('[data-action="toggle-file-visibility"],[data-action="toggle-directory-visibility"]')
                        .trigger('click');
                }
            });*/

            // f — toggle favorite of selected row
            /*data.kpListener.register_combo({
                keys: 'f',
                on_keydown: () => {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    const $row = $(getRows()[data.selectedIndex]);
                    $row
                        .find('[data-action="toggle-file-favorite"],[data-action="toggle-directory-favorite"]')
                        .trigger('click');
                }
            });*/

            // x — delete selected row
            /*data.kpListener.register_combo({
                keys: 'x',
                on_keydown: () => {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    const $row = $(getRows()[data.selectedIndex]);
                    $row
                        .find('[data-action="delete-file"],[data-action="delete-directory"]')
                        .trigger('click');
                }
            });*/

            // Suspend shortcuts while any text input or Bootstrap modal is open.
            // Note: Magnific Popup suspension is handled via callbacks in previewer.js
            // because MFP uses triggerHandler() internally, which does not bubble to document.
            // Arrow keys use native keydown because stop_listening() does not reliably
            // suppress keydown events for non-printable keys. Left/right additionally check
            // MFP state directly as a belt-and-suspenders guard so the lightbox can handle
            // gallery navigation uncontested.
            $(document)
                .on('focusin', 'input, textarea, select, [contenteditable]', suspend)
                .on('focusout', 'input, textarea, select, [contenteditable]', resume)
                .on('show.bs.modal', suspend)
                .on('hidden.bs.modal', resume)
                .on('keydown.keyboard', (e) => {
                    if (data.suspended > 0) {
                        return;
                    }
                    const mfpOpen = $.magnificPopup && $.magnificPopup.instance
                        && $.magnificPopup.instance.isOpen;
                    if (e.key === 'ArrowLeft') {
                        if (!mfpOpen) {
                            history.back();
                        }
                    } else if (e.key === 'ArrowRight') {
                        if (!mfpOpen) {
                            history.forward();
                        }
                    } else if (e.key === 'ArrowUp') {
                        e.preventDefault();
                        selectRow(data.selectedIndex - 1);
                    } else if (e.key === 'ArrowDown') {
                        e.preventDefault();
                        selectRow(data.selectedIndex + 1);
                    }
                });
        };

    // Exports — suspend/resume are called by previewer.js via MFP callbacks.
    self.suspend = suspend;
    self.resume = resume;

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
