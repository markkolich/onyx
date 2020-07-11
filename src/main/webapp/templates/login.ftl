<html>
<head>
    <title>Onyx - Login</title>
    <#include "common/css.ftl">
</head>
<body>

    <div class="container">

        <!-- Outer Row -->
        <div class="row justify-content-center">

          <div class="col-lg-6 col-md-9">

            <div class="card o-hidden border-0 shadow-lg my-5">
              <div class="card-body p-0">
                <!-- Nested Row within Card Body -->
                <div class="row">
                  <div class="col-12">
                    <div class="p-5">
                      <div class="text-center">
                        <h3 class="text-dark mb-4">Onyx</h3>

                      </div>
                      <form action="${contextPath}/login" method="post">
                        <div class="form-group">
                          <input type="text" name="username" class="form-control form-control-user" placeholder="Username" autofocus="autofocus">
                        </div>
                        <div class="form-group">
                          <input type="password" name="password" class="form-control form-control-user" placeholder="Password">
                        </div>
                        <input type="submit" class="btn btn-primary btn-block pt-8" value="Login">
                      </form>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </div>

        </div>

      </div>

    <#include "common/js.ftl">

</body>
</html>