<!-- Topbar -->
<nav class="navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow">

  <a href="${contextPath}/" class="navbar-brand d-flex align-items-center justify-content-center">
    <img src="${contextPath}/static/img/onyx/onyx-logo.svg" width="35" height="35" class="ml-2 d-inline-block align-top" alt="">
    <h3 class="ml-2 my-0 pt-1 text-dark">Onyx</h3>
  </a>

  <#--<#if session?has_content>
      <form action="${contextPath}/search" method="post" class="form-inline mr-auto ml-md-3 my-2 my-md-0 navbar-search">
        <div class="input-group w-100 w-md-50">
          <input type="text" class="form-control bg-light border-0 small" placeholder="Search Files and Folders" aria-label="Search" aria-describedby="basic-addon2">
          <div class="input-group-append">
            <button class="btn btn-dark" type="button">
              <i class="fas fa-search fa-sm"></i>
            </button>
          </div>
        </div>
      </form>
  </#if>-->

  <!-- Topbar Navbar -->
  <ul class="navbar-nav ml-auto">

    <!-- Nav Item - User Information -->
    <#if session?has_content>
        <li class="nav-item dropdown no-arrow">
          <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <span class="text-gray-600 small">${session.getUsername()} <i class="fas fa-user ml-1"></i></span>
          </a>
          <!-- Dropdown - User Information -->
          <div class="dropdown-menu dropdown-menu-right shadow animated--grow-in" aria-labelledby="userDropdown">
            <#--<a class="dropdown-item" href="#">
              <i class="fas fa-user fa-sm fa-fw mr-2 text-gray-400"></i>
              Profile
            </a>
            <a class="dropdown-item" href="#">
              <i class="fas fa-cogs fa-sm fa-fw mr-2 text-gray-400"></i>
              Settings
            </a>
            <a class="dropdown-item" href="#">
              <i class="fas fa-list fa-sm fa-fw mr-2 text-gray-400"></i>
              Activity Log
            </a>-->
            <#if session?has_content>
                <a class="dropdown-item" href="${contextPath}/browse/${session.getUsername()}">
                  <i class="fas fa-home fa-sm fa-fw mr-2 text-gray-400"></i>
                  My Home
                </a>
                <div class="dropdown-divider"></div>
            </#if>
            <a class="dropdown-item" href="${contextPath}/logout">
              <i class="fas fa-sign-out-alt fa-sm fa-fw mr-2 text-gray-400"></i>
              Logout
            </a>
          </div>
        </li>
    <#else>
        <a class="nav-link" href="${contextPath}/login">
          <span class="mr-2 text-gray-600 small">Login</span>
        </a>
    </#if>

  </ul>

</nav>
<!-- End of Topbar -->