<!-- Topbar -->
<nav class="navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow">

  <a href="${contextPath}/<#if session?has_content>browse/${session.getUsername()}</#if>" class="navbar-brand d-flex align-items-center justify-content-center">
    <img src="${contextPath}/static/img/onyx/onyx-logo.svg" width="35" height="35" class="ml-2 d-inline-block align-top" alt="">
    <h3 class="ml-2 my-0 pt-1 text-dark">Onyx</h3>
  </a>

  <!-- Topbar Navbar -->
  <ul class="navbar-nav ml-auto">

    <!-- Nav Item - Dark Mode Toggle -->
    <li class="nav-item no-arrow">
      <a class="nav-link text-muted" href="#" id="darkModeToggle" role="button" title="Toggle Dark Mode">
        <i class="fas fa-moon fa-fw"></i>
      </a>
    </li>

    <#if session?has_content>
        <!-- Nav Item - Search Dropdown -->
        <li class="nav-item dropdown no-arrow">
          <a class="nav-link dropdown-toggle text-muted" href="#" id="searchDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <i class="fas fa-search fa-fw"></i>
          </a>
          <div class="dropdown-menu dropdown-menu-right p-3 shadow animated--grow-in" aria-labelledby="searchDropdown">
            <form action="${contextPath}/search" method="get" class="form-inline mr-auto navbar-search">
                <div class="input-group w-100">
                  <input type="text" name="query" required="required" class="form-control bg-light border-0 small" placeholder="Search files and directories..." aria-label="Search">
                  <div class="input-group-append">
                    <button class="btn btn-primary" type="submit">
                      <i class="fas fa-search fa-sm"></i>
                    </button>
                </div>
              </div>
            </form>
          </div>
        </li>

        <li class="nav-item dropdown no-arrow">
          <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <span class="text-muted">${session.getUsername()} <i class="fas fa-user ml-1"></i></span>
          </a>
          <!-- Dropdown - User Information -->
          <div class="dropdown-menu dropdown-menu-right shadow animated--grow-in" aria-labelledby="userDropdown">
            <#if session?has_content>
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
                <a class="dropdown-item" href="${contextPath}/browse/${session.getUsername()}">
                  <i class="fas fa-home fa-sm fa-fw mr-2 text-gray-400"></i>
                  My Home
                </a>
                <a class="dropdown-item" href="#" id="webauthn-register-passkey" class="d-none">
                  <i class="fas fa-key fa-sm fa-fw mr-2 text-gray-400"></i>
                  Register Passkey
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
        <li class="nav-item no-arrow">
          <a class="nav-link text-muted" href="${contextPath}/login" class="mr-2 text-gray-600 small">Login</a>
        </li>
    </#if>

  </ul>

</nav>
<!-- End of Topbar -->