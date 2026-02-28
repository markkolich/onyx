<#import "../common/macros/detail-card.ftl" as cards>
<@cards.detail_card label="Attributes" icon="fas fa-tags">
    <#if resource.getVisibility() == "PRIVATE">
        <span class="badge badge-dark mr-1">private</span>
    <#else>
        <span class="badge badge-light mr-1">public</span>
    </#if>

    <#if userIsOwner && resource.getFavorite()>
        <span class="badge badge-warning mr-1">favorite</span>
    </#if>

    <#if hasResourceInCache>
        <span class="badge badge-success mr-1">cached</span>
    </#if>
</@cards.detail_card>