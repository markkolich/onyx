/*
 * Copyright (c) 2016 Milan Kyncl
 * Licensed under the MIT license.
 *
 * jquery.copy-to-clipboard plugin
 * https://github.com/mmkyncl/jquery-copy-to-clipboard
 *
 */

$.fn.CopyToClipboard = function() {
    var textToCopy = false;
    if(this.is('select') || this.is('textarea') || this.is('input')){
        textToCopy = this.val();
    }else {
        textToCopy = this.text();
    }
    CopyToClipboard(textToCopy);
};

function CopyToClipboard( val ){
    var hiddenClipboard = $('#_hiddenClipboard_');
    if(!hiddenClipboard.length){
        $('body').append('<textarea style="position:absolute;top: -9999px;" id="_hiddenClipboard_"></textarea>');
        hiddenClipboard = $('#_hiddenClipboard_');
    }
    hiddenClipboard.html(val);
    hiddenClipboard.select();
    document.execCommand('copy');
    document.getSelection().removeAllRanges();
}

$(function(){
    $('[data-clipboard-target]').each(function(){
        $(this).click(function(e) {
            $($(this).data('clipboard-target')).CopyToClipboard();

            e.preventDefault();
            return true;
        });
    });
    $('[data-clipboard-text]').each(function(){
        $(this).click(function(e){
            CopyToClipboard($(this).data('clipboard-text'));

            e.preventDefault();
            return true;
        });
    });
});
