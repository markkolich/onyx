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

                // file is optional - when provided (a drag-and-drop or paste upload), the file is fed
                // straight into the widget below as soon as it's initialized, skipping the manual file
                // picker entirely so the upload starts immediately.
                showModal = (file) => {
                    if (file && uploadInProgress) {
                        // Already committed to a drag-and-drop/paste upload (or still waiting for its
                        // modal to finish opening) - ignore. jQuery's .one('shown.bs.modal', ...) below
                        // registers a brand new one-time listener on every call rather than replacing a
                        // pending one, so without this guard a second call arriving before the first
                        // modal transition finishes would stack a second listener alongside the first,
                        // and both would fire together - feeding two files into the widget and uploading
                        // both concurrently.
                        return;
                    }

                    // Set the form's visibility before the modal is shown, rather than inside the
                    // shown.bs.modal handler below, so there's no flash of the wrong layout as the modal
                    // transitions into view. [data-collapse="true"] elements start hidden in the markup
                    // (upload-file-modal.ftl); idempotent either way regardless of what a previous open
                    // of this same modal left behind.
                    if (file) {
                        // Committing to this upload immediately - marked busy synchronously, right now,
                        // rather than waiting for the fileupload widget's start: callback. Bootstrap
                        // doesn't add .show to the modal element until the backdrop's own fade-in
                        // transition finishes, so a check like $('.modal.show').length (as used by
                        // dropzone.js/pasteboard.js) can't see this modal as "open" for a brief window
                        // right after .modal('show') - see Onyx.App.File.isUploading(), which those
                        // callers check instead to close that gap.
                        uploadInProgress = true;
                        $modal.find('[data-collapse="true"]').addClass('d-none');
                    } else {
                        $modal.find('[data-collapse="true"]').removeClass('d-none');
                    }

                    $modal.one('shown.bs.modal', () => {
                        // Convenience - only meaningful once the modal (and therefore the now-visible
                        // field) is actually shown.
                        if (!file) {
                            // Reset upload state for a fresh manual open.
                            uploadInProgress = false;
                            $modal.find('input[data-file="description"]').focus();
                        }

                        // Prevent the upload form from being submitted manually by the user,
                        // only the file upload plugin should be able to "submit" the form
                        // once a file has been selected.
                        $modal.find('form').unbind().on('submit', (e) => {
                            e.preventDefault();
                            return false;
                        });

                        const $fileInput = $modal.find('input[data-upload="file"]');

                        $fileInput.fileupload({
                            type: 'PUT',
                            singleFileUploads: true,
                            maxNumberOfFiles: 1,
                            multipart: false,
                            // The plugin defaults dropZone to $(document), which - once this widget has
                            // been initialized even once - silently binds its own independent
                            // document-wide drop handler for the rest of the page's life, completely
                            // bypassing dropzone.js's gating (canAcceptDrop()/isUploading()) since it
                            // calls this add: callback directly rather than going through
                            // File.uploadFile()/showModal(). dropzone.js is our own deliberate drop
                            // handler; disable the plugin's redundant, ungated one.
                            dropZone: $(),
                            add: (e, d) => {
                                const rootPath = $('body[data-path]').data('path');
                                const resource = `${rootPath}/${encodeURIComponent(d.files[0].name)}`;

                                const size = d.files[0].size; // size in bytes
                                const description = $modal.find('input[data-file="description"]').val();
                                const visibility = $modal.find('select[data-file="visibility"]').val();

                                $.ajax({
                                    type: 'POST',
                                    url: `${parent.baseApiUrl}/v1/file${resource}?overwrite=true`,
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

                        if (file) {
                            $fileInput.fileupload('add', { files: [file] });
                        }
                    });

                    // Prevent modal from being closed during upload. Uses .on(), not .one() - a
                    // cancelled close attempt (the confirm dialog dismissed with Cancel) must re-arm
                    // for the *next* close attempt too, within this same show cycle, not just catch the
                    // first one. The .off() first avoids stacking a duplicate handler on top of one left
                    // over from an earlier showModal() call.
                    $modal.off('hide.bs.modal').on('hide.bs.modal', (e) => {
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
                'showModal': showModal,
                'isUploading': () => uploadInProgress
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
                    const newFavorite = favorite === false;

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

    // Public API for dropzone.js/pasteboard.js - opens the same upload modal used by the "Upload
    // File" menu action, pre-loaded with a file so it starts uploading immediately instead of
    // waiting on the file picker.
    self.uploadFile = (file) => upload.showModal(file);
    self.isUploading = () => upload.isUploading();

    // Only initialize the application if we're in a context supporting sessions.
    $('body[data-session]').length > 0 && init();

})(Onyx.App || {}, this, this.document);
