<#import "../common/macros/modal-header.ftl" as headers>
<!-- Create Directory modal -->
<div class="modal fade" id="create-directory-modal" tabindex="-1" role="dialog" aria-hidden="true">
<div class="modal-dialog" role="document">
  <form>
    <div class="modal-content">
        <@headers.modal_header title="Create Directory" />
        <div class="modal-body">
          <div class="form-group">
            <label class="col-form-label">Name</label>
            <input type="text" class="form-control" data-directory="name">
          </div>
          <div class="form-group">
            <label class="col-form-label">Description (optional)</label>
            <input type="text" class="form-control" data-directory="description">
          </div>
          <div class="form-group">
            <label class="col-form-label">Visibility</label>
            <select class="form-control" data-directory="visibility">
              <option value="PRIVATE">Private (default)</option>
              <option value="PUBLIC">Public</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" type="button" data-dismiss="modal">Cancel</button>
            <button class="btn btn-primary" type="submit">Create</button>
        </div>
    </div>
  </form>
</div>
</div>