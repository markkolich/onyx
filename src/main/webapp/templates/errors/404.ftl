<!doctype html>
<html lang="en">
<head>
    <title>Onyx <#if devMode>(dev) </#if>- Not Found</title>
    <#include "../common/css.ftl">
</head>

<body class="bg-gray-100">

  <!-- Page Wrapper -->
  <div id="wrapper">

    <!-- Content Wrapper -->
    <div id="content-wrapper" class="d-flex flex-column">

      <!-- Main Content -->
      <div id="content">

        <#include "../common/nav/topbar.ftl">

        <!-- Begin Page Content -->
        <div class="container-fluid text-center">

          <h2>404 Not Found</h2>

          <p>Do you need to <a href="${contextPath}/login">login</a>?</p>

        </div>
        <!-- /.container-fluid -->

      </div>
      <!-- End of Main Content -->

      <#include "../common/nav/footer.ftl">

    </div>
    <!-- End of Content Wrapper -->

  </div>
  <!-- End of Page Wrapper -->

  <#include "../common/js.ftl">

</body>
</html>