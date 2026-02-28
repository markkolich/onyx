<#--
  Renders a detail sidebar card with label, value, and icon.
  - label: the uppercase label text (e.g., "Owner", "Size")
  - icon: FontAwesome icon class (e.g., "fas fa-user")
  - nested content: the value markup to display
-->
<#macro detail_card label icon>
    <div class="card shadow mb-4">
        <div class="card-body">
            <div class="row no-gutters align-items-center">
                <div class="col mr-2">
                    <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">${label}</div>
                    <div class="h5 mb-0 font-weight-bold text-gray-800"><#nested></div>
                </div>
                <div class="col-auto">
                    <i class="${icon} fa-2x text-gray-300"></i>
                </div>
            </div>
        </div>
    </div>
</#macro>
