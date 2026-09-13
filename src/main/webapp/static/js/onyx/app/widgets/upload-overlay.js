(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.UploadOverlay = parent.UploadOverlay || {},

        $overlay = $('#upload-overlay'),

        REJECT_TIMEOUT_MS = 2000,

        canUpload = () => $('body[data-owner][data-resource-type="DIRECTORY"]').length > 0,

        // webkitGetAsEntry() is the de facto standard (despite the prefix, shipped under this exact
        // name in every evergreen browser) way to tell a dropped/pasted folder apart from a file -
        // dataTransfer.files represents a dropped folder as a fake, zero-byte File named after it,
        // which would otherwise silently upload as an empty file. Not guaranteed to exist on every
        // item (clipboard-paste support is spotty), so an item that can't tell us either way is
        // treated as "not a directory" rather than blocking a legitimate upload.
        isDirectoryItem = (item) => {
            if (!item || typeof item.webkitGetAsEntry !== 'function') {
                return false;
            }
            const entry = item.webkitGetAsEntry();
            return !!(entry && entry.isDirectory);
        },

        setMessage = (text) => $overlay.find('[data-upload-overlay="text"]').text(text),

        show = (text) => {
            setMessage(text);
            $overlay.removeClass('d-none upload-overlay-reject');
        },

        showReject = (text) => {
            setMessage(text);
            $overlay.removeClass('d-none').addClass('upload-overlay-reject');
        },

        hide = () => $overlay.addClass('d-none').removeClass('upload-overlay-reject'),

        // For triggers with no natural "end of hover" moment to clear the overlay on (paste, or a
        // rejection discovered only at drop) - shows the rejection, then clears it after a couple
        // seconds on its own.
        showRejectBriefly = (text) => {
            showReject(text);
            window.setTimeout(hide, REJECT_TIMEOUT_MS);
        };

    // The actual upload (POST for a presigned URL, PUT the bytes, progress, reload on done) is
    // handled by the existing "Upload File" modal in file.js - see Onyx.App.File.uploadFile() -
    // rather than a second, parallel implementation here. This widget only owns the lightweight
    // full-page feedback shown while dragging/pasting, before that modal ever opens: the "drop file
    // to upload" invite and the various rejection states.

    self.canUpload = canUpload;
    self.isDirectoryItem = isDirectoryItem;
    self.show = show;
    self.showReject = showReject;
    self.showRejectBriefly = showRejectBriefly;
    self.hide = hide;

})(Onyx.App || {}, this, this.document);
