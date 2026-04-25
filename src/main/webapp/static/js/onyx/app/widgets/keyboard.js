(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.Keyboard = parent.Keyboard || {},

        data = {
            selectedIndex: -1,
            kpListener: null,
            suspended: 0
        },

        suspend = function() {
            if (data.suspended === 0) {
                data.kpListener.stop_listening();
            }
            data.suspended++;
        },

        resume = function() {
            data.suspended = Math.max(0, data.suspended - 1);
            if (data.suspended === 0) {
                data.kpListener.listen();
            }
        },

        getRows = function() {
            return $('tr[data-resource]').toArray();
        },

        selectRow = function(index) {
            var rows = getRows();
            if (!rows.length) {
                return;
            }
            data.selectedIndex = Math.max(0, Math.min(index, rows.length - 1));
            $('tr[data-resource]').removeClass('keyboard-selected');
            rows[data.selectedIndex].classList.add('keyboard-selected');
            rows[data.selectedIndex].scrollIntoView({ block: 'nearest' });
        },

        init = function() {
            data.kpListener = new window.keypress.Listener();

            // / — focus search
            data.kpListener.register_combo({
                keys: '/',
                on_keydown: function(e) {
                    if (e.shiftKey) {
                        return;
                    }
                    $('#searchDropdown').dropdown('toggle');
                    setTimeout(function() {
                        $('.navbar-search input[name="query"]').focus();
                    }, 100);
                }
            });

            // Escape — close any open modal
            data.kpListener.register_combo({
                keys: 'escape',
                on_keydown: function() {
                    $('.modal.show').modal('hide');
                }
            });

            // down arrow — move selection down
            data.kpListener.register_combo({
                keys: 'down',
                on_keydown: function() {
                    selectRow(data.selectedIndex + 1);
                }
            });

            // up arrow — move selection up
            data.kpListener.register_combo({
                keys: 'up',
                on_keydown: function() {
                    selectRow(data.selectedIndex - 1);
                }
            });

            // Enter — open selected row
            data.kpListener.register_combo({
                keys: 'enter',
                on_keydown: function() {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    var rows = getRows();
                    var $link = $(rows[data.selectedIndex]).find('td:nth-child(2) a');
                    if ($link.length) {
                        $link.get(0).click();
                    }
                }
            });

            // d — open details view for selected row
            data.kpListener.register_combo({
                keys: 'd',
                on_keydown: function() {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    var $link = $(getRows()[data.selectedIndex]).find('td:first-child a');
                    if ($link.length) {
                        $link.get(0).click();
                    }
                }
            });

            // u — upload file
            data.kpListener.register_combo({
                keys: 'u',
                on_keydown: function() {
                    $('[data-action="upload-file"]').first()
                        .trigger('click');
                }
            });

            // n — new directory
            data.kpListener.register_combo({
                keys: 'n',
                on_keydown: function() {
                    $('[data-action="create-directory"]').first()
                        .trigger('click');
                }
            });

            // v — toggle visibility of selected row
            data.kpListener.register_combo({
                keys: 'v',
                on_keydown: function() {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    var $row = $(getRows()[data.selectedIndex]);
                    $row
                        .find('[data-action="toggle-file-visibility"],[data-action="toggle-directory-visibility"]')
                        .trigger('click');
                }
            });

            // f — toggle favorite of selected row
            data.kpListener.register_combo({
                keys: 'f',
                on_keydown: function() {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    var $row = $(getRows()[data.selectedIndex]);
                    $row
                        .find('[data-action="toggle-file-favorite"],[data-action="toggle-directory-favorite"]')
                        .trigger('click');
                }
            });

            // x — delete selected row
            data.kpListener.register_combo({
                keys: 'x',
                on_keydown: function() {
                    if (data.selectedIndex < 0) {
                        return;
                    }
                    var $row = $(getRows()[data.selectedIndex]);
                    $row
                        .find('[data-action="delete-file"],[data-action="delete-directory"]')
                        .trigger('click');
                }
            });

            // Suspend shortcuts while any text input, modal, or lightbox is open.
            $(document)
                .on('focusin', 'input, textarea, select, [contenteditable]', suspend)
                .on('focusout', 'input, textarea, select, [contenteditable]', resume)
                .on('show.bs.modal', suspend)
                .on('hidden.bs.modal', resume)
                .on('mfpOpen', suspend)
                .on('mfpAfterClose', resume);
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
