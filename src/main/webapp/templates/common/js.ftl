<#if devMode>
    <script src="${contextPath}/static/build/app.js?${.now?long?c}"></script>
<#else>
    <script src="${contextPath}/static/release/app.min.js?${buildVersion.getBuildNumber()}"></script>
</#if>