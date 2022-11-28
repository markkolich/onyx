<!doctype html>
<html lang="en">
<head>
    <title>Onyx <#if devMode>(dev) </#if>- ${resource.getHtmlPath()}</title>
    <#include "common/css.ftl">
</head>

<body class="bg-gray-100" data-path="${resource.getPath()}" data-description="<#if resource.getHtmlDescription()?has_content>${resource.getHtmlDescription()}</#if>"<#if session?has_content> data-session="${session.id}"</#if>>

  <!-- Page Wrapper -->
  <div id="wrapper">

    <!-- Content Wrapper -->
    <div id="content-wrapper" class="d-flex flex-column">

      <!-- Main Content -->
      <div id="content">

        <#include "common/nav/topbar.ftl">

        <!-- Begin Page Content -->
        <div class="container-fluid">

          <div class="row">

            <div class="col-12">

              <div class="card shadow mb-4">
                <!-- Card Header - Dropdown -->
                <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between">
                  <h4 class="m-0 card-title">
                    <span class="mr-1">/</span>
                    <#list breadcrumbs as crumb>
                      <a href="${contextPath}/browse${crumb.getMiddle()}" class="mr-1">${crumb.getRight()}</a>
                      <#sep><span class="mr-1">/</span></#sep>
                    </#list>
                  </h4>
                  <#if session?has_content>
                      <#-- Controls are only visible if the authenticated user is the owner of the resource. -->
                      <#if userIsOwner>
                          <div class="dropdown no-arrow">
                            <a class="dropdown-toggle" href="#" role="button" id="dropdownMenuLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                              <i class="fas fa-ellipsis-v fa-sm fa-fw text-gray-400"></i>
                            </a>
                            <div class="dropdown-menu dropdown-menu-right shadow animated--fade-in" aria-labelledby="dropdownMenuLink" x-placement="bottom-end" style="position: absolute; will-change: transform; top: 0px; left: 0px; transform: translate3d(17px, 19px, 0px);">
                              <a class="dropdown-item" href="#" data-action="upload-file"><i class="fas fa-upload fa-sm fa-fw mr-2 text-gray-400"></i> Upload File</a>
                              <div class="dropdown-divider"></div>
                              <a class="dropdown-item" href="#" data-action="create-directory"><i class="fas fa-folder-plus fa-sm fa-fw mr-2 text-gray-400"></i> Create Directory</a>
                              <a class="dropdown-item" href="#" data-action="edit-directory"><i class="fas fa-edit fa-sm fa-fw mr-2 text-gray-400"></i> Edit Directory</a>
                              <#if resource.getVisibility() == "PUBLIC">
                                <div class="dropdown-divider"></div>
                                <a class="dropdown-item" href="#" data-action="get-shortlink"><i class="fas fa-link fa-sm fa-fw mr-2 text-gray-400"></i> Get Shortlink</a>
                              </#if>
                            </div>
                          </div>
                      </#if>
                  </#if>
                </div>
                <!-- Card Body -->
                <div class="card-body table-responsive">

                  <#if resource.getHtmlDescription()?has_content>
                      <p class="text-muted mb-3">${resource.getHtmlDescription()}</p>
                  </#if>

                  <#include "common/directory/listing.ftl">

                </div>
              </div>

            </div> <!-- /.col-12 -->

        </div> <!-- /.row -->

      </div> <!-- /.container-fluid -->

      <#include "common/nav/footer.ftl">

    </div>
    <!-- End of Content Wrapper -->

  </div>
  <!-- End of Page Wrapper -->

  <#-- Keep the modals out of the rendered HTML unless there's an active user session on the request. -->
  <#if session?has_content>
    <#include "modals/modals.ftl">
  </#if>

  <#include "common/js.ftl">

</body>
</html>
