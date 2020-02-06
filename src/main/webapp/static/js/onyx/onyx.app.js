(function(parent, window, document) {
    'use strict';

    var

        // Namespace.
        self = parent.App = parent.App || {},

        data = {
            baseAppUrl: parent['baseAppUrl'],
            baseApiUrl: parent['baseApiUrl'],

            $contentDiv: $('#content')
        },

        file = (function() {

            var
                upload = (function() {

                    var
                        modal = $('#uploadFileModal'),

                        showModal = function() {
                            modal.on('shown.bs.modal', function() {
                                // Convenience
                                modal.find('input[data-file="description"]').focus();

                                modal.find('input[data-upload="file"]').fileupload({
                                    type: 'PUT',
                                    singleFileUploads: true,
                                    maxNumberOfFiles: 1,
                                    multipart: false,
                                    add: function(e, d) {
                                        var rootPath = $('body[data-path]').data('path');
                                        var resource = rootPath + '/' + encodeURIComponent(d.files[0].name);

                                        var size = d.files[0].size; // size in bytes
                                        var description = modal.find('input[data-file="description"]').val();
                                        var visibility = modal.find('select[data-file="visibility"]').val();

                                        $.ajax({
                                            type: 'POST',
                                            url: data.baseApiUrl + 'v1/file' + resource,
                                            contentType: 'application/json',
                                            data: JSON.stringify({
                                                size: size,
                                                description: description,
                                                visibility: visibility
                                            }),
                                            success: function(res, status, xhr) {
                                                d.url = res.presignedUploadUrl;

                                                modal.find('[data-collapse="true"]').addClass('d-none');

                                                // Go, upload!
                                                d.submit();
                                            }
                                        });
                                    },
                                    start: function() {
                                        modal.find('div.progress').removeClass('d-none');
                                        modal.find('div.progress .progress-bar')
                                            .css('width', '0%')
                                            .html('');
                                    },
                                    progressall: function(e, d) {
                                        var progress = parseInt(d.loaded / d.total * 100, 10);
                                        modal.find('div.progress .progress-bar')
                                            .css('width', progress + '%')
                                            .html('Uploading: ' + progress + '%');
                                    },
                                    done: function(e) {
                                        // Hide the progress bar, but not needed cuz we refresh the page right
                                        // after a successful upload.
                                        //modal.find('div.progress').addClass('d-none');

                                        window.location.reload(true);
                                    },
                                    fail: function() {
                                        modal.find('[data-collapse="true"]').removeClass('d-none');

                                        modal.find('div.progress .progress-bar')
                                            .css('width', '100%')
                                            .html('Oops, an error occurred: file upload failed.');
                                    }
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
                                url: data.baseApiUrl + 'v1/file' + resource,
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
                                url: data.baseApiUrl + 'v1/file' + resource,
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
                        modal = $('#deleteFileModal'),

                        showModal = function(resource) {
                            var name = decodeURIComponent(resource.split('/').pop());
                            modal.find('[data-modal="name"]').html(name);

                            modal.on('shown.bs.modal', function() {
                                modal.find('button[type="submit"]').unbind().click(function() {
                                    $.ajax({
                                        type: 'DELETE',
                                        url: data.baseApiUrl + 'v1/file' + resource,
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

                }());

            return {
                'upload': upload,
                'update': update,
                'del': del
            };

        }()),

        directory = (function() {

            var
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
                                        url: data.baseApiUrl + 'v1/directory' + resource,
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
                                url: data.baseApiUrl + 'v1/directory' + resource,
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
                                url: data.baseApiUrl + 'v1/directory' + resource,
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
                                        url: data.baseApiUrl + 'v1/directory' + resource,
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

                }());

            return {
                'create': create,
                'update': update,
                'del': del
            };

        }()),

        init = function() {
            // Files

            data.$contentDiv.find('[data-action="upload-file"]').unbind().click(function(e) {
                file.upload.showModal();

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-file-visibility"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                file.update.toggleVisibility(resource, visibility);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-file-favorite"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                file.update.toggleFavorite(resource, favorite);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="delete-file"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                file.del.showModal(resource);

                e.preventDefault();
                return true;
            });

            // Directories

            data.$contentDiv.find('[data-action="create-directory"]').unbind().click(function(e) {
                directory.create.showModal();

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-visibility"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                directory.update.toggleVisibility(resource, visibility);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-directory-favorite"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                directory.update.toggleFavorite(resource, favorite);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="delete-directory"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                directory.del.showModal(resource);

                e.preventDefault();
                return true;
            });

            // Show image resources in a Featherlight lightbox preview.
            data.$contentDiv.find('a[data-resource-type="FILE"]').filter(function() {
                return this.href.match(/\.(gif|jpe?g|tiff|png|webp)$/i);
            }).featherlight({
                type: 'image',
                targetAttr: 'href',
                // No close icon.
                closeIcon: '',
                openSpeed: 100,
                closeSpeed: 100
            });
        };

    init();

})(Onyx || {}, this, this.document);
