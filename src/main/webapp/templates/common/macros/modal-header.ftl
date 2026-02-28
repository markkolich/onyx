<#--
  Renders a standard Bootstrap modal header with title and close button.
  - title: the modal title text
-->
<#macro modal_header title>
    <div class="modal-header">
        <h5 class="modal-title">${title}</h5>
        <button class="close" type="button" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true"><i class="fas fa-times fa-sm"></i></span>
        </button>
    </div>
</#macro>
