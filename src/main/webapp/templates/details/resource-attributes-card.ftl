<div class="card shadow mb-4">
  <div class="card-body">
      <div class="row no-gutters align-items-center">
          <div class="col mr-2">
              <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">Attributes</div>
              <div class="h5 mb-0 font-weight-bold text-gray-800">
                <#if resource.getVisibility() == "PRIVATE">
                  <span class="badge badge-dark mr-1">private</span>
                <#else>
                  <span class="badge badge-light mr-1">public</span>
                </#if>

                <#if userIsOwner && resource.getFavorite()>
                  <span class="badge badge-warning mr-1">favorite</span>
                </#if>

                <#if hasResourceInCache>
                  <span class="badge badge-success mr-1">cached</span>
                </#if>
              </div>
          </div>
          <div class="col-auto">
              <i class="fas fa-tags fa-2x text-gray-300"></i>
          </div>
      </div>
  </div>
</div>