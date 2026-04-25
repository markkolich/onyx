(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Directory = parent.Directory || {},

        data = {
            $contentDiv: $('#content')
        },

        create = (function() {

            const
                $modal = $('#create-directory-modal'),

                showModal = () => {
                    $modal.one('shown.bs.modal', () => {
                        // Convenience
                        $modal.find('input[data-directory="name"]').focus();

                        $modal.find('form').unbind().on('submit', (e) => {
                            e.preventDefault();

                            const rootPath = $('body[data-path]').data('path');
                            const name = $modal.find('input[data-directory="name"]').val();
                            const resource = `${rootPath}/${encodeURIComponent(name)}`;

                            const description = $modal.find('input[data-directory="description"]').val();
                            const visibility = $modal.find('select[data-directory="visibility"]').val();

                            $.ajax({
                                type: 'POST',
                                url: `${parent.baseApiUrl}/v1/directory${resource}`,
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    description: description,
                                    visibility: visibility
                                }),
                                success: () => {
                                    $modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });

                            return false;
                        });
                    });

                    $modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        edit = (function() {

            const
                toggleVisibility = (resource, visibility) => {
                    const newVisibility = (visibility === 'PUBLIC') ? 'PRIVATE' : 'PUBLIC';

                    $.ajax({
                        type: 'PUT',
                        url: `${parent.baseApiUrl}/v1/directory${resource}`,
                        contentType: 'application/json',
                        data: JSON.stringify({
                            visibility: newVisibility
                        }),
                        success: () => {
                            window.location.reload(true);
                        }
                    });
                },

                toggleFavorite = (resource, favorite) => {
                    const newFavorite = (favorite === false) ? true : false;

                    $.ajax({
                        type: 'PUT',
                        url: `${parent.baseApiUrl}/v1/directory${resource}`,
                        contentType: 'application/json',
                        data: JSON.stringify({
                            favorite: newFavorite
                        }),
                        success: () => {
                            window.location.reload(true);
                        }
                    });
                };

            return {
                'toggleVisibility': toggleVisibility,
                'toggleFavorite': toggleFavorite
            };

        }()),

        del = (function() {

            const $modal = $('#delete-directory-modal');
            let permanent = false;

            const showModal = (resource) => {
                const name = decodeURIComponent(resource.split('/').pop());
                $modal.find('[data-modal="name"]').text(name);

                const kpListener = new window.keypress.Listener();
                permanent = false;

                $modal.one('shown.bs.modal', () => {
                    const $submitButton = $modal.find('button[type="submit"]');
                    kpListener.register_combo({
                        'keys': 'shift',
                        'is_exclusive': true,
                        'on_keydown': () => {
                            $submitButton.text('Delete Permanently');
                            permanent = true;
                        },
                        'on_keyup': () => {
                            $submitButton.text('Delete');
                            permanent = false;
                        }
                    });

                    $modal.find('button[type="submit"]').unbind().click(() => {
                        kpListener.stop_listening();

                        $.ajax({
                            type: 'DELETE',
                            url: `${parent.baseApiUrl}/v1/directory${resource}?${$.param({'permanent': permanent})}`,
                            success: () => {
                                $modal.modal('hide');

                                window.location.reload(true);
                            }
                        });
                    });
                });

                $modal.one('hidden.bs.modal', () => {
                    kpListener.reset();
                    kpListener.destroy();
                });

                $modal.modal('show');
            };

            return {
                'showModal': showModal
            };

        }()),

        init = () => {
            data.$contentDiv.find('[data-action="create-directory"]').on('click', (e) => {
                e.preventDefault();

                create.showModal();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-visibility"]').on('click', function(e) {
                e.preventDefault();

                const resource = $(this).closest('tr[data-resource]').data('resource');
                const visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                edit.toggleVisibility(resource, visibility);
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-favorite"]').on('click', function(e) {
                e.preventDefault();

                const resource = $(this).closest('tr[data-resource]').data('resource');
                const favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                edit.toggleFavorite(resource, favorite);
                return true;
            });
            data.$contentDiv.find('[data-action="delete-directory"]').on('click', function(e) {
                e.preventDefault();

                const resource = $(this).closest('tr[data-resource]').data('resource');
                del.showModal(resource);
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
