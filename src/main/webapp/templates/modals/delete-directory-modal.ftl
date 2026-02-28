<#import "../common/macros/modal-header.ftl" as headers>
<!-- Delete Directory modal-->
<div class="modal fade" id="delete-directory-modal" tabindex="-1" role="dialog" aria-hidden="true">
<div class="modal-dialog" role="document">
  <div class="modal-content">
    <@headers.modal_header title="Delete Directory" />
    <div class="modal-body">
        <p>Are you sure you want to delete this directory?</p>
        <code data-modal="name"></code>
    </div>
    <div class="modal-footer">
        <button class="btn btn-secondary" type="button" data-dismiss="modal">Cancel</button>
        <button class="btn btn-danger" type="submit">Delete</button>
    </div>
  </div>
</div>
</div>