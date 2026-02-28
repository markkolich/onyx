<#--
  Renders a resource icon (folder or file) with filled/outline style based on visibility.
  - type: "DIRECTORY" or "FILE"
  - visibility: "PRIVATE" or "PUBLIC"
  - href: link target URL
  PRIVATE resources use filled (fas) icons; PUBLIC use outline (far) icons.
-->
<#macro resource_icon type visibility href>
    <#if type == "DIRECTORY">
        <a href="${href}"><i class="pl-2 <#if visibility == "PRIVATE">fas<#else>far</#if> fa-folder text-body"></i></a>
    <#elseif type == "FILE">
        <a href="${href}"><i class="pl-2 <#if visibility == "PRIVATE">fas<#else>far</#if> fa-file text-body"></i></a>
    </#if>
</#macro>

<#--
  Renders a folder-open icon for directory listing headers.
  - visibility: "PRIVATE" or "PUBLIC"
  - href: link target URL
-->
<#macro folder_open_icon visibility href>
    <a href="${href}"><i class="pl-2 <#if visibility == "PRIVATE">fas<#else>far</#if> fa-folder-open text-body"></i></a>
</#macro>
