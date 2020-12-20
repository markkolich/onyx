<!doctype html>
<html lang="en">
<head>
    <title>Onyx - ${directory.getHtmlPath()}</title>
    <#include "common/css.ftl">
</head>

<body class="bg-gray-100" data-path="${directory.getPath()}"<#if session?has_content> data-session="${session.id}"</#if>>

  <!-- Page Wrapper -->
  <div id="wrapper">

    <!-- Content Wrapper -->
    <div id="content-wrapper" class="d-flex flex-column">

      <!-- Main Content -->
      <div id="content">

        <#include "common/nav/topbar.ftl">

        <!-- Begin Page Content -->
        <div class="container-fluid">

          <div class="card shadow mb-4">
              <!-- Card Header - Dropdown -->
              <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between">
                <h4 class="m-0 card-title"><span class="mr-1">/</span><#list breadcrumbs as crumb><a href="${contextPath}/browse${crumb.getMiddle()}" class="mr-1">${crumb.getRight()}</a><#sep><span class="mr-1">/</span></#sep></#list></h4>
                <#if session?has_content>
                    <#-- Controls are only visible if the authenticated user is the owner of the resource. -->
                    <#if directory.getOwner() == session.getUsername()>
                        <div class="dropdown no-arrow">
                          <a class="dropdown-toggle" href="#" role="button" id="dropdownMenuLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            <i class="fas fa-ellipsis-v fa-sm fa-fw text-gray-400"></i>
                          </a>
                          <div class="dropdown-menu dropdown-menu-right shadow animated--fade-in" aria-labelledby="dropdownMenuLink" x-placement="bottom-end" style="position: absolute; will-change: transform; top: 0px; left: 0px; transform: translate3d(17px, 19px, 0px);">
                            <a class="dropdown-item" href="#" data-action="upload-file"><i class="fas fa-upload fa-sm fa-fw mr-2 text-gray-400"></i> Upload File</a>
                            <a class="dropdown-item" href="#" data-action="create-directory"><i class="fas fa-folder-plus fa-sm fa-fw mr-2 text-gray-400"></i> Create Directory</a>
                          </div>
                        </div>
                    </#if>
                </#if>
              </div>
              <!-- Card Body -->
              <div class="card-body table-responsive">

                <#if directory.getHtmlDescription()?has_content>
                    <p class="text-muted mb-3">${directory.getHtmlDescription()}</p>
                </#if>

                <table class="table table-striped table-hover mb-0">

                    <#if children?has_content>
                        <thead>
                            <tr>
                                <td class="align-middle">
                                    <#if directory.getVisibility() == "PRIVATE">
                                        <i class="pl-2 fas fa-folder-open text-dark"></i>
                                    <#else>
                                        <i class="pl-2 far fa-folder-open"></i>
                                    </#if>
                                </td>
                                <td class="align-middle" colspan="5">
                                    <span>${directory.getHtmlName()}</span>
                                    <#if (directoryCount+fileCount > 0)>
                                        <p class="text-muted small mb-0">
                                            <#if (directoryCount > 0)>
                                                ${directoryCount} <#if (directoryCount > 1)>directories<#else>directory</#if>
                                                <#if (fileCount > 0)>,&nbsp;</#if>
                                            </#if>
                                            <#if (fileCount > 0)>
                                                ${fileCount} file<#if (fileCount > 1)>s</#if> (${totalFileDisplaySize})
                                            </#if>
                                        </p>
                                    </#if>
                                </td>
                            </tr>
                        </thead>
                    </#if>

                    <tbody>
                        <#if children?has_content>
                            <#list children as child>
                                <tr data-resource="${child.getPath()}" data-resource-visibility="${child.getVisibility()}" data-resource-favorite="${child.getFavorite()?then('true','false')}">
                                    <td class="align-middle">
                                        <#if child.getType() == "DIRECTORY">
                                            <#if child.getVisibility() == "PRIVATE">
                                                <i class="pl-2 fas fa-folder text-dark"></i>
                                            <#else>
                                                <i class="pl-2 far fa-folder"></i>
                                            </#if>
                                        <#elseif child.getType() == "FILE">
                                            <#if child.getVisibility() == "PRIVATE">
                                                <i class="pl-2 fas fa-file text-dark"></i>
                                            <#else>
                                                <i class="pl-2 far fa-file"></i>
                                            </#if>
                                        </#if>
                                    </td>
                                    <td class="align-middle">
                                        <#if child.getType() == "DIRECTORY">
                                            <a href="${contextPath}/browse${child.getPath()}" data-resource-type="DIRECTORY">${child.getHtmlName()?truncate(40, '...')}</a>
                                        <#elseif child.getType() == "FILE">
                                            <a href="${contextPath}/file${child.getPath()}" data-resource-type="FILE">${child.getHtmlName()?truncate(40, '...')}</a>
                                        </#if>
                                    </td>
                                    <td class="align-middle d-none d-lg-table-cell">
                                        <#if child.getHtmlDescription()?has_content>
                                            ${child.getHtmlDescription()?truncate(40, '...')}
                                        <#else>
                                            &nbsp;
                                        </#if>
                                    </td>
                                    <td class="align-middle d-none d-lg-table-cell">${child.getCreatedAt()?datetime?string["MMM dd, yyyy, h:mm a"]}</td>
                                    <td class="align-middle d-none d-lg-table-cell">
                                        <#if child.getType() == "FILE">
                                            ${child.getHtmlSize()}
                                        <#else>
                                            &nbsp;
                                        </#if>
                                    </td>

                                    <td class="align-middle text-right">
                                        <#if session?has_content>
                                            <div class="btn-group" role="group">
                                                <#-- Controls are only visible if the authenticated user is the owner of the resource. -->
                                                <#if child.getOwner() == session.getUsername()>
                                                    <#if child.getType() == "DIRECTORY">
                                                        <button type="button" class="btn btn-sm <#if child.getVisibility() == "PUBLIC">btn-dark<#else>btn-light</#if>" data-action="toggle-directory-visibility"><i class="<#if child.getVisibility() == "PUBLIC">fas fa-user-secret<#else>far fa-eye</#if> fa-fw"></i></button>
                                                        <button type="button" class="btn btn-sm btn-warning" data-action="toggle-directory-favorite"><i class="<#if child.getFavorite()>fas<#else>far</#if> fa-star fa-fw"></i></button>
                                                        <button type="button" class="btn btn-sm btn-danger" data-action="delete-directory"><i class="fas fa-trash fa-fw"></i></button>
                                                    <#elseif child.getType() == "FILE">
                                                        <button type="button" class="btn btn-sm <#if child.getVisibility() == "PUBLIC">btn-dark<#else>btn-light</#if>" data-action="toggle-file-visibility"><i class="<#if child.getVisibility() == "PUBLIC">fas fa-user-secret<#else>far fa-eye</#if> fa-fw" fa-fw"></i></button>
                                                        <button type="button" class="btn btn-sm btn-warning" data-action="toggle-file-favorite"><i class="<#if child.getFavorite()>fas<#else>far</#if> fa-star fa-fw"></i></button>
                                                        <button type="button" class="btn btn-sm btn-danger" data-action="delete-file"><i class="fas fa-trash fa-fw"></i></button>
                                                    </#if>
                                                </#if>
                                            </div>
                                        <#else>
                                            &nbsp;
                                        </#if>
                                    </td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                <td><i class="far fa-smile-wink"></i> Move along, nothing to see here.</td>
                            </tr>
                        </#if>
                    </tbody>

                  </table>

              </div>
          </div>

        </div>
        <!-- /.container-fluid -->

      </div>
      <!-- End of Main Content -->

      <#include "common/nav/footer.ftl">

    </div>
    <!-- End of Content Wrapper -->

  </div>
  <!-- End of Page Wrapper -->

  <#-- Keep the modals out of the rendered HTML unless there's an active user session on the request. -->
  <#if session?has_content>
      <#include "modals/upload-file-modal.ftl">
      <#include "modals/delete-file-modal.ftl">
      <#include "modals/create-directory-modal.ftl">
      <#include "modals/delete-directory-modal.ftl">
  </#if>

  <#include "common/js.ftl">

</body>
</html>