<#import "../common/macros/modal-header.ftl" as headers>
<!-- Edit Directory modal -->
<div class="modal fade" id="edit-directory-modal" tabindex="-1" role="dialog" aria-hidden="true">
<div class="modal-dialog" role="document">
  <form>
      <div class="modal-content">
        <@headers.modal_header title="Edit Directory" />
        <div class="modal-body">
          <div class="form-group">
            <label class="col-form-label">Description (optional)</label>
            <input type="text" class="form-control" data-directory="description">
          </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" type="button" data-dismiss="modal">Cancel</button>
            <button class="btn btn-primary" type="submit">Save</button>
        </div>
      </div>
  </form>
</div>
</div>