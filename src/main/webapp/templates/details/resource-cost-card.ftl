<#import "../common/macros/detail-card.ftl" as cards>
<#if resource.getHtmlCost()??>
    <@cards.detail_card label="Cost/month" icon="fas fa-dollar-sign"><code class="text-success">${resource.getHtmlCost()}</code></@cards.detail_card>
</#if>