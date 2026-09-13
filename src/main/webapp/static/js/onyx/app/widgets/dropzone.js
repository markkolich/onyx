(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Dropzone = parent.Dropzone || {},

        $document = $(document),

        // Drag events fire on every child element too; track enter/leave depth so the overlay
        // doesn't flicker when dragging over children.
        state = { depth: 0 },

        isFileDrag = (e) =>
            !!(e.originalEvent.dataTransfer &&
               Array.prototype.indexOf.call(e.originalEvent.dataTransfer.types || [], 'Files') !== -1),

        // Checking .modal.show alone isn't enough - Bootstrap doesn't add that class to the upload
        // modal until its backdrop finishes fading in, a brief window after File.uploadFile() is
        // called during which a second drop could otherwise sneak through. isUploading() is set
        // synchronously the moment an upload is committed to, closing that gap.
        canAcceptDrop = () => $('.modal.show').length === 0 && !parent.File.isUploading(),

        // Best-effort file count *before* drop. dataTransfer.items reports dragged item count (and
        // kind: 'file' vs 'string') during dragenter/dragover in all evergreen browsers, even though
        // the actual File objects aren't readable until drop. Used only to show early feedback; the
        // authoritative check happens again at drop.
        countDraggedFiles = (e) => {
            const items = e.originalEvent.dataTransfer.items;
            if (!items) {
                return null; // unknown until drop
            }
            return Array.prototype.filter.call(items, (item) => item.kind === 'file').length;
        },

        resetDepth = () => {
            state.depth = 0;
        },

        init = () => {
            // Every handler below preventDefault()s as soon as it knows this is a genuine file drag,
            // *before* checking canAcceptDrop() - without that, an in-progress-upload or
            // already-modal-open drag falls through to the browser's native handling (e.g. it
            // navigates the tab to display the dropped file directly), which looks indistinguishable
            // from "the drop still worked." Blocking only changes whether we react to the drag, never
            // whether we own the event.
            $document.on('dragenter', (e) => {
                if (!isFileDrag(e)) {
                    return;
                }
                e.preventDefault();
                if (!canAcceptDrop()) {
                    return;
                }
                state.depth += 1;

                // Note: folders can't be detected here. webkitGetAsEntry() - the only way to tell a
                // folder from a file - requires actual filesystem access that browsers withhold until
                // drop, the same restriction that keeps getAsFile() from working this early.
                // item.kind/items.length are just safe metadata, which is why the file count above can
                // be checked this early but "is this a folder" can't.
                const count = countDraggedFiles(e);
                if (count !== null && count > 1) {
                    parent.UploadOverlay.showReject('Only one file can be uploaded at a time');
                } else {
                    parent.UploadOverlay.show('Drop file to upload');
                }
            });

            $document.on('dragover', (e) => {
                if (!isFileDrag(e)) {
                    return;
                }
                e.preventDefault();
            });

            $document.on('dragleave', (e) => {
                if (!isFileDrag(e)) {
                    return;
                }
                e.preventDefault();
                if (!canAcceptDrop()) {
                    return;
                }
                state.depth -= 1;
                if (state.depth <= 0) {
                    resetDepth();
                    parent.UploadOverlay.hide();
                }
            });

            $document.on('drop', (e) => {
                if (!isFileDrag(e)) {
                    return;
                }
                e.preventDefault();
                if (!canAcceptDrop()) {
                    return;
                }

                const items = e.originalEvent.dataTransfer.items;
                const files = e.originalEvent.dataTransfer.files;
                resetDepth();
                parent.UploadOverlay.hide();

                // Authoritative check - the pre-drop item count above is best-effort feedback only,
                // this is what actually gates the upload. Multi-file drops never upload anything, even
                // the first file - they're rejected outright.
                if (files.length !== 1) {
                    return;
                }

                // Unlike the file count, a folder can only be detected here, at drop - see the note in
                // the dragenter handler above - so this is the first and only place that can tell the
                // user why nothing is about to upload.
                if (items && parent.UploadOverlay.isDirectoryItem(items[0])) {
                    parent.UploadOverlay.showRejectBriefly('Folders cannot be uploaded');
                    return;
                }

                parent.File.uploadFile(files[0]);
            });
        };

    // Only initialize on a directory page the current user owns - dropping a file onto a file's
    // details page must never upload (nothing to upload "into", and it'd read as an accidental
    // overwrite risk).
    parent.UploadOverlay.canUpload() && init();

})(Onyx.App || {}, this, this.document);
