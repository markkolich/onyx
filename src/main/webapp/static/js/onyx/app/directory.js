(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.Directory = parent.Directory || {},

        data = {
            $contentDiv: $('#content')
        },

        create = (function() {

            var
                $modal = $('#create-directory-modal'),

                showModal = function() {
                    $modal.on('shown.bs.modal', function() {
                        // Convenience
                        $modal.find('input[data-directory="name"]').focus();

                        $modal.find('form').unbind().on('submit', function(e) {
                            e.preventDefault();

                            var rootPath = $('body[data-path]').data('path');
                            var name = $modal.find('input[data-directory="name"]').val();
                            var resource = rootPath + '/' + encodeURIComponent(name);

                            var description = $modal.find('input[data-directory="description"]').val();
                            var visibility = $modal.find('select[data-directory="visibility"]').val();

                            $.ajax({
                                type: 'POST',
                                url: parent.baseApiUrl + '/v1/directory' + resource,
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    description: description,
                                    visibility: visibility
                                }),
                                success: function(res, status, xhr) {
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

            var
                $modal = $('#edit-directory-modal'),

                showModal = function() {
                    var description = $('body[data-description]').data('description');
                    $modal.find('input[data-directory="description"]').val(description);

                    $modal.on('shown.bs.modal', function() {
                        // Convenience
                        $modal.find('input[data-directory="description"]').focus();

                        $modal.find('form').unbind().on('submit', function(e) {
                            e.preventDefault();

                            var resource = $('body[data-path]').data('path');

                            var newDescription = $modal.find('input[data-directory="description"]').val();

                            $.ajax({
                                type: 'PUT',
                                url: parent.baseApiUrl + '/v1/directory' + resource,
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    description: newDescription
                                }),
                                success: function(res, status, xhr) {
                                    $modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });

                            return false;
                        });
                    });

                    $modal.modal('show');
                },

                toggleVisibility = function(resource, visibility) {
                    var newVisibility = (visibility === 'PUBLIC') ? 'PRIVATE' : 'PUBLIC';

                    $.ajax({
                        type: 'PUT',
                        url: parent.baseApiUrl + '/v1/directory' + resource,
                        contentType: 'application/json',
                        data: JSON.stringify({
                            visibility: newVisibility
                        }),
                        success: function(res, status, xhr) {
                            window.location.reload(true);
                        }
                    });
                },

                toggleFavorite = function(resource, favorite) {
                    var newFavorite = (favorite === false) ? true : false;

                    $.ajax({
                        type: 'PUT',
                        url: parent.baseApiUrl + '/v1/directory' + resource,
                        contentType: 'application/json',
                        data: JSON.stringify({
                            favorite: newFavorite
                        }),
                        success: function(res, status, xhr) {
                            window.location.reload(true);
                        }
                    });
                };

            return {
                'showModal': showModal,
                'toggleVisibility': toggleVisibility,
                'toggleFavorite': toggleFavorite
            };

        }()),

        del = (function() {

            var
                $modal = $('#delete-directory-modal'),
                permanent = false,

                showModal = function(resource) {
                    var name = decodeURIComponent(resource.split('/').pop());
                    $modal.find('[data-modal="name"]').html(name);

                    var kpListener = new window.keypress.Listener();
                    permanent = false;

                    $modal.on('shown.bs.modal', function() {
                        var $submitButton = $modal.find('button[type="submit"]');
                        kpListener.register_combo({
                            'keys': 'shift',
                            'is_exclusive': true,
                            'on_keydown': function() {
                                $submitButton.text('Delete Permanently');
                                permanent = true;
                            },
                            'on_keyup': function() {
                                $submitButton.text('Delete');
                                permanent = false;
                            }
                        });

                        $modal.find('button[type="submit"]').unbind().click(function() {
                            kpListener.stop_listening();

                            $.ajax({
                                type: 'DELETE',
                                url: parent.baseApiUrl + '/v1/directory' + resource
                                    + '?' + $.param({'permanent':permanent}),
                                success: function(res, status, xhr) {
                                    $modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });
                        });
                    });

                    $modal.on('hidden.bs.modal', function() {
                        kpListener.reset();
                        kpListener.destroy();
                    });

                    $modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        init = function() {
            data.$contentDiv.find('[data-action="create-directory"]').on('click', function(e) {
                e.preventDefault();

                create.showModal();
                return true;
            });
            data.$contentDiv.find('[data-action="edit-directory"]').on('click', function(e) {
                e.preventDefault();

                edit.showModal();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-visibility"]').on('click', function(e) {
                e.preventDefault();

                var resource = $(this).closest('tr[data-resource]').data('resource');
                var visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                edit.toggleVisibility(resource, visibility);
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-favorite"]').on('click', function(e) {
                e.preventDefault();

                var resource = $(this).closest('tr[data-resource]').data('resource');
                var favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                edit.toggleFavorite(resource, favorite);
                return true;
            });
            data.$contentDiv.find('[data-action="delete-directory"]').on('click', function(e) {
                e.preventDefault();

                var resource = $(this).closest('tr[data-resource]').data('resource');
                del.showModal(resource);
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
