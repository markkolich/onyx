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
                modal = $('#createDirectoryModal'),

                showModal = function() {
                    modal.on('shown.bs.modal', function() {
                        // Convenience
                        modal.find('input[data-directory="name"]').focus();

                        modal.find('button[type="submit"]').unbind().click(function() {
                            var rootPath = $('body[data-path]').data('path');
                            var name = modal.find('input[data-directory="name"]').val();
                            var resource = rootPath + '/' + encodeURIComponent(name);

                            var description = modal.find('input[data-directory="description"]').val();
                            var visibility = modal.find('select[data-directory="visibility"]').val();

                            $.ajax({
                                type: 'POST',
                                url: parent.baseApiUrl + 'v1/directory' + resource,
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    description: description,
                                    visibility: visibility
                                }),
                                success: function(res, status, xhr) {
                                    modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });
                        });
                    });

                    modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        update = (function() {

            var
                toggleVisibility = function(resource, visibility) {
                    var newVisibility = (visibility === 'PUBLIC') ? 'PRIVATE' : 'PUBLIC';

                    $.ajax({
                        type: 'PUT',
                        url: parent.baseApiUrl + 'v1/directory' + resource,
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
                        url: parent.baseApiUrl + 'v1/directory' + resource,
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
                'toggleVisibility': toggleVisibility,
                'toggleFavorite': toggleFavorite
            };

        }()),

        del = (function() {

            var
                modal = $('#deleteDirectoryModal'),

                showModal = function(resource) {
                    var name = decodeURIComponent(resource.split('/').pop());
                    modal.find('[data-modal="name"]').html(name);

                    modal.on('shown.bs.modal', function() {
                        modal.find('button[type="submit"]').unbind().click(function() {
                            $.ajax({
                                type: 'DELETE',
                                url: parent.baseApiUrl + 'v1/directory' + resource,
                                success: function(res, status, xhr) {
                                    modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });
                        });
                    });

                    modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        init = function() {
            data.$contentDiv.find('[data-action="create-directory"]').unbind().click(function(e) {
                create.showModal();

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-visibility"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                update.toggleVisibility(resource, visibility);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-favorite"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                update.toggleFavorite(resource, favorite);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="delete-directory"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                del.showModal(resource);

                e.preventDefault();
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
