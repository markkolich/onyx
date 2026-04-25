(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.File = parent.File || {},

        data = {
            $contentDiv: $('#content')
        },

        upload = (function() {

            const $modal = $('#upload-file-modal');
            let uploadInProgress = false;

            const
                beforeUnloadHandler = (e) => {
                    if (uploadInProgress) {
                        const confirmationMessage = 'File upload in progress. Are you sure you ' +
                            'want to leave? Upload will be interrupted.';
                        e.returnValue = confirmationMessage;
                        return confirmationMessage;
                    }
                },

                showModal = () => {
                    $modal.one('shown.bs.modal', () => {
                        // Reset upload state
                        uploadInProgress = false;

                        // Convenience
                        $modal.find('input[data-file="description"]').focus();

                        // Prevent the upload form from being submitted manually by the user,
                        // only the file upload plugin should be able to "submit" the form
                        // once a file has been selected.
                        $modal.find('form').unbind().on('submit', (e) => {
                            e.preventDefault();
                            return false;
                        });

                        $modal.find('input[data-upload="file"]').fileupload({
                            type: 'PUT',
                            singleFileUploads: true,
                            maxNumberOfFiles: 1,
                            multipart: false,
                            add: (e, d) => {
                                const rootPath = $('body[data-path]').data('path');
                                const resource = `${rootPath}/${encodeURIComponent(d.files[0].name)}`;

                                const size = d.files[0].size; // size in bytes
                                const description = $modal.find('input[data-file="description"]').val();
                                const visibility = $modal.find('select[data-file="visibility"]').val();

                                $.ajax({
                                    type: 'POST',
                                    url: `${parent.baseApiUrl}/v1/file${resource}`,
                                    contentType: 'application/json',
                                    data: JSON.stringify({
                                        size: size,
                                        description: description,
                                        visibility: visibility
                                    }),
                                    success: (res) => {
                                        d.url = res.presignedUploadUrl;

                                        $modal.find('[data-collapse="true"]').addClass('d-none');

                                        // Go, upload!
                                        d.submit();
                                    }
                                });
                            },
                            start: () => {
                                // Set upload in progress and attach beforeunload handler
                                uploadInProgress = true;
                                $(window).on('beforeunload', beforeUnloadHandler);

                                $modal.find('div.progress').removeClass('d-none');
                                $modal.find('div.progress .progress-bar')
                                    .css('width', '0%')
                                    .html('');
                            },
                            progressall: (e, d) => {
                                const progress = parseInt(d.loaded / d.total * 100, 10);
                                $modal.find('div.progress .progress-bar')
                                    .css('width', `${progress}%`)
                                    .html(`Uploading: ${progress}%`);
                            },
                            done: () => {
                                // Upload complete - clear state and handler
                                uploadInProgress = false;
                                $(window).off('beforeunload', beforeUnloadHandler);

                                // Hide the progress bar, but not needed cuz we refresh the page right
                                // after a successful upload.
                                //$modal.find('div.progress').addClass('d-none');

                                window.location.reload(true);
                            },
                            fail: () => {
                                // Upload failed - clear state and handler
                                uploadInProgress = false;
                                $(window).off('beforeunload', beforeUnloadHandler);

                                $modal.find('[data-collapse="true"]').removeClass('d-none');

                                $modal.find('div.progress .progress-bar')
                                    .css('width', '100%')
                                    .html('Oops, an error occurred: file upload failed.');
                            }
                        });
                    });

                    // Prevent modal from being closed during upload
                    $modal.one('hide.bs.modal', (e) => {
                        if (uploadInProgress) {
                            if (!confirm('File upload in progress. Are you sure you want to close? ' +
                                    'Upload will be interrupted.')) {
                                e.preventDefault();
                                return false;
                            } else {
                                // User confirmed - clean up
                                uploadInProgress = false;
                                $(window).off('beforeunload', beforeUnloadHandler);
                            }
                        }
                    });

                    // Clean up handler if modal is closed without upload
                    $modal.one('hidden.bs.modal', () => {
                        uploadInProgress = false;
                        $(window).off('beforeunload', beforeUnloadHandler);
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
                        url: `${parent.baseApiUrl}/v1/file${resource}`,
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
                        url: `${parent.baseApiUrl}/v1/file${resource}`,
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

            const $modal = $('#delete-file-modal');
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
                            url: `${parent.baseApiUrl}/v1/file${resource}?${$.param({'permanent': permanent})}`,
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
            data.$contentDiv.find('[data-action="upload-file"]').on('click', (e) => {
                e.preventDefault();

                upload.showModal();
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-file-visibility"]').on('click', function(e) {
                e.preventDefault();

                const resource = $(this).closest('tr[data-resource]').data('resource');
                const visibility = $(this).closest('tr[data-resource-visibility]').data('resource-visibility');
                edit.toggleVisibility(resource, visibility);
                return true;
            });
            data.$contentDiv.find('[data-action="toggle-file-favorite"]').on('click', function(e) {
                e.preventDefault();

                const resource = $(this).closest('tr[data-resource]').data('resource');
                const favorite = $(this).closest('tr[data-resource-favorite]').data('resource-favorite');
                edit.toggleFavorite(resource, favorite);
                return true;
            });
            data.$contentDiv.find('[data-action="delete-file"]').on('click', function(e) {
                e.preventDefault();

                const resource = $(this).closest('tr[data-resource]').data('resource');
                del.showModal(resource);
                return true;
            });
        };

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
