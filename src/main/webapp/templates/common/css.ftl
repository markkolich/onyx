<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">

<link rel="icon" type="image/png" href="${contextPath}/static/img/onyx/onyx-logo.png">

<#if devMode>
    <link href="${contextPath}/static/build/app.css?v=${.now?long?c}" rel="stylesheet">
<#else>
    <link href="${contextPath}/static/release/app.min.css?v=${buildVersion.getBuildNumber()}" rel="stylesheet">
</#if>