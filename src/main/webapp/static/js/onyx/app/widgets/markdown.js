(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Markdown = parent.Markdown || {},

        data = {
            resource: $('body').data('path'),
            $editable: $('[data-editable="description"]'),
            $rendered: $('[data-editable="description"] .rendered-markdown')
        },

        renderer = (function() {

            const
                configure = () => {
                    marked.use({
                        renderer: {
                            // Double underscores render as underline instead of bold.
                            strong: function(token) {
                                const content = this.parser.parseInline(token.tokens);
                                if (token.raw.indexOf('__') === 0) {
                                    return `<u>${content}</u>`;
                                }
                                return `<strong>${content}</strong>`;
                            },
                            // Add Bootstrap table class to rendered tables.
                            table: function(token) {
                                const self = this;
                                const renderCell = (cell) => {
                                    const align = cell.align ? ` style="text-align:${cell.align}"` : '';
                                    return `<td${align}>${self.parser.parseInline(cell.tokens)}</td>`;
                                };
                                const headerCells = token.header.map(renderCell).join('\n');
                                const bodyRows = token.rows.map((row) => {
                                    return `<tr>${row.map(renderCell).join('\n')}</tr>`;
                                }).join('\n');
                                return '<table class="table table-striped table-hover mb-0">\n' +
                                    `<thead><tr>${headerCells}</tr></thead>\n` +
                                    `<tbody>${bodyRows}</tbody>\n</table>\n`;
                            },
                            // Strip images, images are not supported.
                            image: () => '',
                            // Escape raw HTML so it renders as literal text as HTML is not supported in this editor.
                            html: (token) => token.raw
                                .replace(/&/g, '&amp;')
                                .replace(/</g, '&lt;')
                                .replace(/>/g, '&gt;')
                        }
                    });
                },

                render = () => {
                    $('[data-raw-description]').each(function() {
                        const $el = $(this);
                        const raw = $el.attr('data-raw-description');
                        if (raw) {
                            let $target = $el.find('.rendered-markdown');
                            if ($target.length === 0) {
                                $target = $el.filter('.rendered-markdown');
                            }
                            if ($target.length > 0) {
                                $target.html(DOMPurify.sanitize(marked.parse(raw)));
                            }
                        }
                    });
                },

                init = () => {
                    configure();
                    render();
                };

            return {
                'init': init
            };

        }()),

        inlineEdit = (function() {

            let editing = false,
                rawDescription = '',
                kpListener = null;

            const
                enterEditMode = () => {
                    if (editing) {
                        return;
                    }
                    editing = true;

                    const $textarea = $('<textarea>').attr('rows', 1).val(rawDescription);
                    data.$rendered.hide();
                    data.$editable.append($textarea);

                    const autoResize = () => {
                        const scrollX = window.pageXOffset;
                        const scrollY = window.pageYOffset;
                        $textarea[0].style.height = '0';
                        $textarea[0].style.height = `${$textarea[0].scrollHeight}px`;
                        window.scrollTo(scrollX, scrollY);
                    };
                    autoResize();
                    $textarea.on('input', autoResize);
                    $textarea.focus();

                    $textarea.on('blur', () => {
                        save($textarea.val());
                    });

                    kpListener = new window.keypress.Listener($textarea[0]);
                    kpListener.register_combo({
                        'keys': 'escape',
                        'on_keydown': () => {
                            cancelEditMode();
                        }
                    });
                },

                cancelEditMode = () => {
                    if (kpListener) {
                        kpListener.reset();
                        kpListener.destroy();
                        kpListener = null;
                    }
                    editing = false;
                    data.$editable.find('textarea').remove();
                    data.$rendered.show();
                },

                save = (newValue) => {
                    if (newValue === rawDescription) {
                        cancelEditMode();
                        return;
                    }

                    const resourceType = data.$editable.attr('data-resource-type') || 'DIRECTORY';
                    const apiPath = (resourceType === 'FILE') ? '/v1/file' : '/v1/directory';

                    $.ajax({
                        type: 'PUT',
                        url: `${parent.baseApiUrl}${apiPath}${data.resource}`,
                        contentType: 'application/json',
                        data: JSON.stringify({
                            description: newValue
                        }),
                        success: () => {
                            rawDescription = newValue;
                            if (newValue) {
                                data.$rendered.html(DOMPurify.sanitize(marked.parse(newValue)));
                            } else {
                                data.$rendered.html('<em class="text-muted">Click to add a description...</em>');
                            }
                            cancelEditMode();
                        },
                        error: () => {
                            cancelEditMode();
                        }
                    });
                },

                init = () => {
                    if (data.$editable.length === 0) {
                        return;
                    }

                    rawDescription = data.$editable.attr('data-raw-description') || '';

                    data.$editable.on('click', (e) => {
                        if (!$(e.target).is('textarea')) {
                            enterEditMode();
                        }
                    });
                };

            return {
                'init': init
            };

        }()),

        init = () => {
            renderer.init();

            // Only enable inline editing for authenticated users.
            $('body[data-session]').length > 0 && inlineEdit.init();
        };

    init();

})(Onyx.App || {}, this, this.document);
