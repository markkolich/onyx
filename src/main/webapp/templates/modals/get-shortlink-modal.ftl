<#import "../common/macros/modal-header.ftl" as headers>
<!-- Get Shortlink modal -->
<div class="modal fade" id="get-shortlink-modal" tabindex="-1" role="dialog" aria-hidden="true">
<div class="modal-dialog" role="document">
  <form>
    <div class="modal-content">
        <@headers.modal_header title="Get Shortlink" />
        <div class="modal-body">
          <div class="form-group">
            <input type="text" class="form-control" data-shortlink="link" readonly="readonly">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-primary" type="button" data-dismiss="modal">Close</button>
        </div>
    </div>
  </form>
</div>
</div>