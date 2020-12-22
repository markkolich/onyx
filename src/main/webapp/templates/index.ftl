<!doctype html>
<html lang="en">
<head>
    <title>Onyx - Home</title>
    <#include "common/css.ftl">
</head>

<body class="bg-gray-100">

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
                <h4 class="m-0 card-title">Home</h4>
              </div>
              <!-- Card Body -->
              <div class="card-body table-responsive">

                <table class="table table-striped table-hover mb-0">
                    <tbody>
                        <#if children?has_content>
                            <#list children as child>
                                <tr data-resource="${child.getPath()}">
                                    <td class="align-middle">
                                        <#if child.getType() == "DIRECTORY">
                                            <a href="${contextPath}/details${child.getPath()}"><i class="pl-2 far fa-folder text-body"></i></a>
                                        <#elseif child.getType() == "FILE">
                                            <a href="${contextPath}/details${child.getPath()}"><i class="pl-2 far fa-file text-body"></i></a>
                                        </#if>
                                    </td>
                                    <td class="align-middle">
                                        <#if child.getType() == "DIRECTORY">
                                            <a href="${contextPath}/browse${child.getPath()}">${child.getHtmlName()}</a>
                                        <#elseif child.getType() == "FILE">
                                            <a href="${contextPath}/file${child.getPath()}">${child.getHtmlName()}</a>
                                        </#if>
                                    </td>
                                    <td class="align-middle d-none d-lg-table-cell">
                                        <#if child.getHtmlDescription()?has_content>
                                            ${child.getHtmlDescription()?truncate(40, '...')}
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

  <#include "common/js.ftl">

</body>
</html>