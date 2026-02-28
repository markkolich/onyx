<#import "../common/macros/detail-card.ftl" as cards>
<#if resource.getLastAccessedDate()??>
    <@cards.detail_card label="Last Accessed At" icon="fas fa-calendar">${resource.getLastAccessedDate()?datetime?string["MMM dd, yyyy, h:mm a"]}</@cards.detail_card>
</#if>