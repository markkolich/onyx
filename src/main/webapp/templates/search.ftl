<!doctype html>
<html lang="en">
<head>
    <title>Onyx <#if devMode>(dev) </#if>- Search - ${query}</title>
    <#include "common/css.ftl">
</head>

<body class="bg-gray-100 dark-mode"<#if session?has_content> data-session="${session.id}"</#if>>

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
                  <h4 class="m-0 card-title">${query}</h4>
                </div>
                <!-- Card Body -->
                <div class="card-body table-responsive">

                  <table class="table table-striped table-hover mb-0">

                      <tbody>
                          <#if results?has_content>
                              <#list results as result>
                                  <tr data-resource="${result.getLeft().getPath()}" data-resource-visibility="${result.getLeft().getVisibility()}" data-resource-favorite="${result.getLeft().getFavorite()?then('true','false')}">
                                      <td class="align-middle">
                                          <#if result.getLeft().getType() == "DIRECTORY">
                                              <#if result.getLeft().getVisibility() == "PRIVATE">
                                                  <a href="${contextPath}/details${result.getLeft().getPath()}"><i class="pl-2 fas fa-folder text-body"></i></a>
                                              <#else>
                                                  <a href="${contextPath}/details${result.getLeft().getPath()}"><i class="pl-2 far fa-folder text-body"></i></a>
                                              </#if>
                                          <#elseif result.getLeft().getType() == "FILE">
                                              <#if result.getLeft().getVisibility() == "PRIVATE">
                                                  <a href="${contextPath}/details${result.getLeft().getPath()}"><i class="pl-2 fas fa-file text-body"></i></a>
                                              <#else>
                                                  <a href="${contextPath}/details${result.getLeft().getPath()}"><i class="pl-2 far fa-file text-body"></i></a>
                                              </#if>
                                          </#if>
                                      </td>
                                      <td class="align-middle">
                                        <span class="mr-1">/</span>
                                        <#list result.getRight() as crumb>
                                          <#if crumb?is_last>
                                            <#if result.getLeft().getType() == "DIRECTORY">
                                              <a href="${contextPath}/browse${crumb.getMiddle()}" data-resource-type="DIRECTORY" class="mr-1">${crumb.getRight()}</a>
                                            <#elseif result.getLeft().getType() == "FILE">
                                              <a href="${contextPath}/api/v1/download${crumb.getMiddle()}" data-resource-type="FILE" class="mr-1">${crumb.getRight()}</a>
                                            </#if>
                                          <#else>
                                            <a href="${contextPath}/browse${crumb.getMiddle()}" data-resource-type="DIRECTORY" class="mr-1">${crumb.getRight()}</a>
                                          </#if>
                                          <#sep><span class="mr-1">/</span></#sep>
                                        </#list>
                                      </td>
                                      <td class="align-middle d-none d-lg-table-cell">
                                          <#if result.getLeft().getHtmlDescription()?has_content>
                                              <span title="${result.getLeft().getHtmlDescription()}" data-clipboard-text="${result.getLeft().getHtmlDescription()}">${result.getLeft().getHtmlDescription()?truncate(100, '...')}</span>
                                          <#else>
                                              <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
                                          </#if>
                                      </td>
                                      <td class="align-middle d-none d-lg-table-cell text-nowrap text-muted">
                                          ${result.getLeft().getHtmlSize()}
                                      </td>

                                      <td class="align-middle d-none d-lg-table-cell text-right">
                                          <#if result.getLeft().getVisibility() == "PRIVATE">
                                            <span class="badge badge-dark mr-1">private</span>
                                          <#else>
                                            <span class="badge badge-light mr-1">public</span>
                                          </#if>
                                          <#if result.getLeft().getFavorite()>
                                            <span class="badge badge-warning mr-1">favorite</span>
                                          </#if>
                                      </td>
                                  </tr>
                              </#list>
                          <#else>
                              <tr>
                                  <td><i class="far fa-sad-cry"></i> No results found.</td>
                              </tr>
                          </#if>
                      </tbody>

                    </table>

                </div>
              </div>

            </div> <!-- /.col-12 -->

          </div> <!-- /.row -->

        </div> <!-- /.container-fluid -->

      </div> <!-- /main content -->

      <#include "common/nav/footer.ftl">

    </div>
    <!-- End of Content Wrapper -->

  </div>
  <!-- End of Page Wrapper -->

  <#include "common/js.ftl">

</body>
</html>
