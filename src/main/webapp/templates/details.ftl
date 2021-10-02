<!doctype html>
<html lang="en">
<head>
    <title>Onyx - Details - ${resource.getHtmlPath()}</title>
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

            <div class="col-xl-8 col-lg-12">

              <div class="card shadow mb-4">
                <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between">
                  <h4 class="m-0 card-title">
                    <#list breadcrumbs?reverse?chunk(3)?first?reverse as crumb>
                      <#if crumb?is_last>
                        <span class="mr-1">${crumb.getRight()}</span>
                      <#else>
                        <a href="${contextPath}/details${crumb.getMiddle()}" class="mr-1">${crumb.getRight()}</a>
                      </#if>
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
                           <#if resource.getType() == "DIRECTORY">
                             <a class="dropdown-item" href="#" data-action="upload-file"><i class="fas fa-upload fa-sm fa-fw mr-2 text-gray-400"></i> Upload File</a>
                             <div class="dropdown-divider"></div>
                             <a class="dropdown-item" href="#" data-action="create-directory"><i class="fas fa-folder-plus fa-sm fa-fw mr-2 text-gray-400"></i> Create Directory</a>
                             <a class="dropdown-item" href="#" data-action="edit-directory"><i class="fas fa-edit fa-sm fa-fw mr-2 text-gray-400"></i> Edit Directory</a>
                           <#elseif resource.getType() == "FILE">
                             <a class="dropdown-item" href="#" data-action="edit-file"><i class="fas fa-edit fa-sm fa-fw mr-2 text-gray-400"></i> Edit File</a>
                           </#if>
                         </div>
                       </div>
                     </#if>
                   </#if>
                </div>
                <div class="card-body table-responsive">

                  <#if resource.getHtmlDescription()?has_content>
                    <p class="text-muted mb-3">${resource.getHtmlDescription()}</p>
                  </#if>

                  <#if resource.getType() == "DIRECTORY">
                    <#include "common/directory-listing.ftl">
                  <#elseif resource.getType() == "FILE">
                    <a href="${contextPath}/file${resource.getPath()}?nocache=1" class="btn btn-primary btn-icon-split mb-0">
                      <span class="icon text-white-50"><i class="fas fa-file-download"></i></span>
                      <span class="text">Download</span>
                    </a>
                  </#if>
                </div>
              </div>

            </div> <!-- /.col-xl-8 -->

            <div class="col-xl-4 col-lg-12">

              <div class="card shadow mb-4">
                <div class="card-body">
                    <div class="row no-gutters align-items-center">
                        <div class="col mr-2">
                          <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">Owner</div>
                          <div class="h5 mb-0 font-weight-bold text-gray-800">${resource.getOwner()}</div>
                        </div>
                        <div class="col-auto">
                          <i class="fas fa-user fa-2x text-gray-300"></i>
                        </div>
                    </div>
                </div>
              </div>

              <div class="card shadow mb-4">
                <div class="card-body">
                    <div class="row no-gutters align-items-center">
                        <div class="col mr-2">
                          <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">Created At</div>
                          <div class="h5 mb-0 font-weight-bold text-gray-800">${resource.getCreatedDate()?datetime?string["MMM dd, yyyy, h:mm a"]}</div>
                        </div>
                        <div class="col-auto">
                          <i class="fas fa-calendar fa-2x text-gray-300"></i>
                        </div>
                    </div>
                </div>
              </div>

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

              <#if resource.getType() == "FILE">
                <div class="card shadow mb-4">
                  <div class="card-body">
                      <div class="row no-gutters align-items-center">
                          <div class="col mr-2">
                              <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">File Size</div>
                              <div class="h5 mb-0 font-weight-bold text-gray-800">${resource.getHtmlSize()}</div>
                          </div>
                          <div class="col-auto">
                              <i class="fas fa-hdd fa-2x text-gray-300"></i>
                          </div>
                      </div>
                  </div>
                </div>
              </#if>

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

            </div> <!-- /.col-xl-4 -->

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