<#import "../common/macros/detail-card.ftl" as cards>
<@cards.detail_card label="Type" icon="fas fa-file-code">
    <#if resource.getType() == "FILE">
        <#if contentType?has_content>${contentType}</#if>
    <#else>
        Directory
    </#if>
</@cards.detail_card>