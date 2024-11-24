<html>
<head>
    <title>Onyx <#if devMode>(dev) </#if>- Login - Verify your Identity</title>
    <#include "common/css.ftl">
</head>
<body class="dark-mode">

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
                        <h5>Verify your Identity</h5>
                        <p>Please enter the verification code sent to the mobile phone number for this account.</p>
                      </div>
                      <form action="${contextPath}/login/verify" method="post">
                        <input type="hidden" name="token" value="${token}">
                        <div class="form-group">
                          <input type="text" maxlength="32" name="code" class="form-control form-control-user" placeholder="Code" autofocus="autofocus" required="required">
                        </div>
                        <div class="form-group">
                          <div class="custom-control custom-checkbox text-center">
                            <input type="checkbox" class="custom-control-input" id="trustDevice" name="trustDevice">
                            <label class="custom-control-label" for="trustDevice">Trust this device</label>
                          </div>
                        </div>
                        <input type="submit" class="btn btn-primary btn-block pt-8" value="Verify">
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