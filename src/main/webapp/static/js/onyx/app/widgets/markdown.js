(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.Markdown = parent.Markdown || {},

        data = {
            resource: $('body').data('path'),
            $editable: $('[data-editable="description"]'),
            $rendered: $('[data-editable="description"] .rendered-markdown')
        },

        renderer = (function() {

            var
                configure = function() {
                    marked.use({
                        renderer: {
                            // Double underscores render as underline instead of bold.
                            strong: function(token) {
                                var content = this.parser.parseInline(token.tokens);
                                if (token.raw.indexOf('__') === 0) {
                                    return '<u>' + content + '</u>';
                                }
                                return '<strong>' + content + '</strong>';
                            },
                            // Add Bootstrap table class to rendered tables.
                            table: function(token) {
                                var self = this;
                                var renderCell = function(cell) {
                                    var align = cell.align ? ' style="text-align:' + cell.align + '"' : '';
                                    return '<td' + align + '>' + self.parser.parseInline(cell.tokens) + '</td>';
                                };
                                var headerCells = token.header.map(renderCell).join('\n');
                                var bodyRows = token.rows.map(function(row) {
                                    return '<tr>' + row.map(renderCell).join('\n') + '</tr>';
                                }).join('\n');
                                return '<table class="table table-striped table-hover mb-0">\n' +
                                    '<thead><tr>' + headerCells + '</tr></thead>\n' +
                                    '<tbody>' + bodyRows + '</tbody>\n</table>\n';
                            },
                            // Strip images, images are not supported.
                            image: function() {
                                return '';
                            },
                            // Escape raw HTML so it renders as literal text as HTML is not supported in this editor.
                            html: function(token) {
                                return token.raw.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                            }
                        }
                    });
                },

                render = function() {
                    $('[data-raw-description]').each(function() {
                        var $el = $(this);
                        var raw = $el.attr('data-raw-description');
                        if (raw) {
                            var $target = $el.find('.rendered-markdown');
                            if ($target.length === 0) {
                                $target = $el.filter('.rendered-markdown');
                            }
                            if ($target.length > 0) {
                                $target.html(marked.parse(raw));
                            }
                        }
                    });
                },

                init = function() {
                    configure();
                    render();
                };

            return {
                'init': init
            };

        }()),

        inlineEdit = (function() {

            var
                editing = false,
                rawDescription = '',

                kpListener = null,

                enterEditMode = function() {
                    if (editing) {
                        return;
                    }
                    editing = true;

                    var $textarea = $('<textarea>').attr('rows', 1).val(rawDescription);
                    data.$rendered.hide();
                    data.$editable.append($textarea);

                    var autoResize = function() {
                        var scrollX = window.pageXOffset;
                        var scrollY = window.pageYOffset;
                        $textarea[0].style.height = '0';
                        $textarea[0].style.height = $textarea[0].scrollHeight + 'px';
                        window.scrollTo(scrollX, scrollY);
                    };
                    autoResize();
                    $textarea.on('input', autoResize);
                    $textarea.focus();

                    $textarea.on('blur', function() {
                        save($textarea.val());
                    });

                    kpListener = new window.keypress.Listener($textarea[0]);
                    kpListener.register_combo({
                        'keys': 'escape',
                        'on_keydown': function() {
                            cancelEditMode();
                        }
                    });
                },

                cancelEditMode = function() {
                    if (kpListener) {
                        kpListener.reset();
                        kpListener.destroy();
                        kpListener = null;
                    }
                    editing = false;
                    data.$editable.find('textarea').remove();
                    data.$rendered.show();
                },

                save = function(newValue) {
                    if (newValue === rawDescription) {
                        cancelEditMode();
                        return;
                    }

                    var resourceType = data.$editable.attr('data-resource-type') || 'DIRECTORY';
                    var apiPath = (resourceType === 'FILE') ? '/v1/file' : '/v1/directory';

                    $.ajax({
                        type: 'PUT',
                        url: parent.baseApiUrl + apiPath + data.resource,
                        contentType: 'application/json',
                        data: JSON.stringify({
                            description: newValue
                        }),
                        success: function() {
                            rawDescription = newValue;
                            if (newValue) {
                                data.$rendered.html(marked.parse(newValue));
                            } else {
                                data.$rendered.html('<em class="text-muted">Click to add a description...</em>');
                            }
                            cancelEditMode();
                        },
                        error: function() {
                            cancelEditMode();
                        }
                    });
                },

                init = function() {
                    if (data.$editable.length === 0) {
                        return;
                    }

                    rawDescription = data.$editable.attr('data-raw-description') || '';

                    data.$editable.on('click', function(e) {
                        if (!$(e.target).is('textarea')) {
                            enterEditMode();
                        }
                    });
                };

            return {
                'init': init
            };

        }()),

        init = function() {
            renderer.init();

            // Only enable inline editing for authenticated users.
            $('body[data-session]').length > 0 && inlineEdit.init();
        };

    init();

})(Onyx.App || {}, this, this.document);
