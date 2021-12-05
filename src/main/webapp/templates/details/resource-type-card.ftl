<div class="card shadow mb-4">
  <div class="card-body">
      <div class="row no-gutters align-items-center">
          <div class="col mr-2">
              <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">Type</div>
              <div class="h5 mb-0 font-weight-bold text-gray-800">
                <#if resource.getType() == "FILE">
                  <#if contentType?has_content>${contentType}</#if>
                <#else>
                  Directory
                </#if>
              </div>
          </div>
          <div class="col-auto">
              <i class="fas fa-file-code fa-2x text-gray-300"></i>
          </div>
      </div>
  </div>
</div>