<#if resource.getHtmlCost()??>
    <div class="card shadow mb-4">
        <div class="card-body">
            <div class="row no-gutters align-items-center">
                <div class="col mr-2">
                  <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">Cost/month</div>
                  <div class="h5 mb-0 font-weight-bold text-gray-800"><code class="text-success">${resource.getHtmlCost()}</code></div>
                </div>
                <div class="col-auto">
                  <i class="fas fa-dollar-sign fa-2x text-gray-300"></i>
                </div>
            </div>
        </div>
    </div>
</#if>
