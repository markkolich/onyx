<!-- Upload File modal-->
<div class="modal fade" id="upload-file-modal" tabindex="-1" role="dialog" aria-hidden="true">
<div class="modal-dialog" role="document">
  <div class="modal-content">
    <div class="modal-header">
      <h5 class="modal-title">Upload File</h5>
      <button class="close" type="button" data-dismiss="modal" aria-label="Close">
        <span aria-hidden="true"><i class="fas fa-times fa-sm"></i></span>
      </button>
    </div>
    <div class="modal-body">
        <form>
            <div class="form-group" data-collapse="true">
                <label class="col-form-label">Description (optional)</label>
                <input type="text" class="form-control" data-file="description">
            </div>
            <div class="form-group" data-collapse="true">
                <label class="col-form-label">Visibility</label>
                <select class="form-control" data-file="visibility">
                    <option value="PRIVATE">Private (default)</option>
                    <option value="PUBLIC">Public</option>
                </select>
            </div>
            <div class="form-group">
                <label class="btn btn-sm btn-block btn-primary mt-4" data-collapse="true">
                    <i class="fas fa-upload fa-fw mr-2"></i>
                    <span>Select &amp; upload...</span>
                    <input class="d-none" data-upload="file" type="file" name="files[]">
                </label>
                <div class="progress my-1 d-none">
                    <div class="progress-bar progress-bar-primary progress-bar-striped progress-bar-animated">&nbsp;</div>
                </div>
            </div>
        </form>
    </div>
  </div>
</div>
</div>