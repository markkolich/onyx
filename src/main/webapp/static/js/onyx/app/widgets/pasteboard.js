(function(parent, window, document) {
    'use strict';

    const

        // Namespace
        self = parent.Pasteboard = parent.Pasteboard || {},

        $document = $(document),

        EXTENSION_FROM_NAME_PATTERN = /\.([a-z0-9]{1,8})$/i,

        EXTENSION_BY_MIME_TYPE = {
            'image/png': 'png',
            'image/jpeg': 'jpg',
            'image/gif': 'gif',
            'image/webp': 'webp',
            'image/svg+xml': 'svg',
            'image/bmp': 'bmp',
            'image/tiff': 'tiff',
            'text/plain': 'txt',
            'text/html': 'html',
            'application/pdf': 'pdf',
            'application/json': 'json'
        },

        // Real files pasted from Finder/Explorer often keep their original name even when the browser
        // reports a generic MIME type for them (e.g. a .zip commonly comes through as
        // application/octet-stream), so the name's own extension is trusted first. Falls back to the
        // MIME type only when the name has none. Returns null - never a guess like "bin" - when
        // neither source tells us anything, so the caller can reject rather than upload a file whose
        // type is a complete unknown.
        resolveExtension = (blob) => {
            const nameMatch = EXTENSION_FROM_NAME_PATTERN.exec(blob.name || '');
            if (nameMatch) {
                return nameMatch[1].toLowerCase();
            }

            const mimeType = blob.type || '';
            if (EXTENSION_BY_MIME_TYPE[mimeType]) {
                return EXTENSION_BY_MIME_TYPE[mimeType];
            }

            const subtype = mimeType.split('/')[1] || '';
            return (/^[a-z0-9]+$/i).test(subtype) ? subtype.toLowerCase() : null;
        },

        // crypto.randomUUID() needs a secure context (HTTPS, or localhost for local dev) - falls back
        // to a timestamp + random suffix otherwise.
        uniqueToken = () => {
            if (window.crypto && typeof window.crypto.randomUUID === 'function') {
                return window.crypto.randomUUID();
            }
            return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
        },

        // The base name is always synthesized (never the blob's own name), since a screenshot's name
        // is typically a generic, collision-prone placeholder like "image.png" - only the extension
        // is worth keeping.
        buildPastedFilename = (extension) => `pasted-${uniqueToken()}.${extension}`,

        // Don't hijack paste while the user is typing/pasting into a text field or the inline
        // description editor - only clipboard *files* pasted outside of an editable context should
        // trigger an upload.
        isEditableFocus = () => {
            const el = document.activeElement;
            if (!el) {
                return false;
            }
            return el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable;
        },

        // Checking .modal.show alone isn't enough - see the matching comment in dropzone.js for why
        // isUploading() is also required.
        canAcceptPaste = () => $('.modal.show').length === 0 && !isEditableFocus() && !parent.File.isUploading(),

        init = () => {
            $document.on('paste', (e) => {
                const clipboardData = e.originalEvent.clipboardData;
                if (!clipboardData) {
                    return;
                }

                const fileItems = Array.prototype.filter.call(
                    clipboardData.items || [], (item) => item.kind === 'file');

                if (fileItems.length === 0) {
                    return; // nothing file-shaped on the clipboard - let normal paste happen
                }

                // Own the event now that it's a genuine file paste, before checking canAcceptPaste() -
                // same reasoning as dropzone.js's preventDefault-before-checking pattern, so a blocked
                // paste (upload already in progress, a modal open, editable focus) is inert rather than
                // falling through to whatever the browser would otherwise do with a pasted file.
                e.preventDefault();

                if (!canAcceptPaste()) {
                    return;
                }

                if (fileItems.length > 1) {
                    parent.UploadOverlay.showRejectBriefly('Only one file can be pasted at a time');
                    return;
                }

                if (parent.UploadOverlay.isDirectoryItem(fileItems[0])) {
                    parent.UploadOverlay.showRejectBriefly('Folders cannot be uploaded');
                    return;
                }

                const blob = fileItems[0].getAsFile();
                if (!blob) {
                    return;
                }

                const extension = resolveExtension(blob);
                if (!extension) {
                    parent.UploadOverlay.showRejectBriefly('Could not determine the pasted file type');
                    return;
                }

                // File.name is read-only, so a rename means constructing a new File from the pasted
                // blob's bytes.
                const file = new window.File([blob], buildPastedFilename(extension), { type: blob.type });
                parent.File.uploadFile(file);
            });
        };

    // Same gate as dropzone.js - directory page, owned by the current user.
    parent.UploadOverlay.canUpload() && init();

})(Onyx.App || {}, this, this.document);
