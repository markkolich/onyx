(function(parent, window, document) {
    'use strict';

    var

        // Namespace
        self = parent.File = parent.File || {},

        data = {
            $contentDiv: $('#content')
        },

        upload = (function() {

            var
                modal = $('#upload-file-modal'),

                showModal = function() {
                    modal.on('shown.bs.modal', function() {
                        // Convenience
                        modal.find('input[data-file="description"]').focus();

                        // Prevent the upload form from being submitted manually by the user,
                        // only the file upload plugin should be able to "submit" the form
                        // once a file has been selected.
                        modal.find('form').unbind().on('submit', function(e) {
                            e.preventDefault();
                            return false;
                        });

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
                                    url: parent.baseApiUrl + '/v1/file' + resource,
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

        edit = (function() {

            var
                modal = $('#edit-file-modal'),

                showModal = function() {
                    var description = $('body[data-description]').data('description');
                    modal.find('input[data-file="description"]').val(description);

                    modal.on('shown.bs.modal', function() {
                        // Convenience
                        modal.find('input[data-file="description"]').focus();

                        modal.find('form').unbind().on('submit', function(e) {
                            var resource = $('body[data-path]').data('path');

                            var newDescription = modal.find('input[data-file="description"]').val();

                            $.ajax({
                                type: 'PUT',
                                url: parent.baseApiUrl + '/v1/file' + resource,
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    description: newDescription
                                }),
                                success: function(res, status, xhr) {
                                    modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });

                            e.preventDefault();
                            return false;
                        });
                    });

                    modal.modal('show');
                },

                toggleVisibility = function(resource, visibility) {
                    var newVisibility = (visibility === 'PUBLIC') ? 'PRIVATE' : 'PUBLIC';

                    $.ajax({
                        type: 'PUT',
                        url: parent.baseApiUrl + '/v1/file' + resource,
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
                        url: parent.baseApiUrl + '/v1/file' + resource,
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
                modal = $('#delete-file-modal'),
                permanent = false,

                showModal = function(resource) {
                    var name = decodeURIComponent(resource.split('/').pop());
                    modal.find('[data-modal="name"]').html(name);

                    var kpListener = new window.keypress.Listener();
                    permanent = false;

                    modal.on('shown.bs.modal', function() {
                        var submitButton = modal.find('button[type="submit"]');
                        kpListener.register_combo({
                            'keys': 'shift',
                            'is_exclusive': true,
                            'on_keydown': function() {
                                submitButton.text('Delete Permanently');
                                permanent = true;
                            },
                            'on_keyup': function() {
                                submitButton.text('Delete');
                                permanent = false;
                            }
                        });

                        modal.find('button[type="submit"]').unbind().click(function() {
                            kpListener.stop_listening();

                            $.ajax({
                                type: 'DELETE',
                                url: parent.baseApiUrl + '/v1/file' + resource
                                    + '?' + $.param({'permanent':permanent}),
                                success: function(res, status, xhr) {
                                    modal.modal('hide');

                                    window.location.reload(true);
                                }
                            });
                        });
                    });

                    modal.on('hidden.bs.modal', function() {
                        kpListener.reset();
                        kpListener.destroy();
                    });

                    modal.modal('show');
                };

            return {
                'showModal': showModal
            };

        }()),

        init = function() {
            data.$contentDiv.find('[data-action="upload-file"]').unbind().click(function(e) {
                upload.showModal();

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="edit-file"]').unbind().click(function(e) {
                edit.showModal();

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-file-visibility"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                edit.toggleVisibility(resource, visibility);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-file-favorite"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                var favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                edit.toggleFavorite(resource, favorite);

                e.preventDefault();
                return true;
            });
            data.$contentDiv.find('[data-action="delete-file"]').unbind().click(function(e) {
                var resource = $(this).closest('tr[data-resource]').data('resource');
                del.showModal(resource);

                e.preventDefault();
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
