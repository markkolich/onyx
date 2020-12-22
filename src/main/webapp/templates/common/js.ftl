<#if devMode>
    <script src="${contextPath}/static/build/app.js?v=${.now?long?c}"></script>
<#else>
    <script src="${contextPath}/static/release/app.min.js?v=${buildVersion.getBuildNumber()}"></script>
</#if>