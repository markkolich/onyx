<table class="table table-striped table-hover mb-0">

  <#if allChildren?has_content>
      <thead>
          <tr>
              <td class="align-middle">
                  <#if resource.getVisibility() == "PRIVATE">
                      <a href="${contextPath}/<#if view == "browse">details<#else>browse</#if>${resource.getPath()}"><i class="pl-2 fas fa-folder-open text-body"></i></a>
                  <#else>
                      <a href="${contextPath}/<#if view == "browse">details<#else>browse</#if>${resource.getPath()}"><i class="pl-2 far fa-folder-open text-body"></i></a>
                  </#if>
              </td>
              <td class="align-middle" <#if view == "browse">colspan="4"<#else>colspan="5"</#if>>
                  <span>${resource.getHtmlName()}</span>
                  <#if (directoryCount+fileCount > 0)>
                      <p class="text-muted small mb-0">
                          <#if (directoryCount > 0)>
                              ${directoryCount} <#if (directoryCount > 1)>directories<#else>directory</#if>
                              <#if (fileCount > 0)>,&nbsp;</#if>
                          </#if>
                          <#if (fileCount > 0)>
                              ${fileCount} file<#if (fileCount > 1)>s</#if>
                          </#if>
                          <#--
                            Only show the user the total size of the resource and all
                            of its children if the authenticated user is the owner.
                          -->
                          <#if userIsOwner>
                              &nbsp;(${totalFileDisplaySize})
                          </#if>
                      </p>
                  </#if>
              </td>
          </tr>
      </thead>
  </#if>

  <#if allChildren?has_content>
    <#if userIsOwner>
      <#--
        The directory listing can only be shown sorted by favorite if the user is
        the owner of the directory resource.
      -->
      <#if favorites?has_content>
        <tbody>
          <#list favorites as child>
            <#include "listing-tbody.ftl">
          </#list>
        </tbody>
      </#if>
      <#if nonFavorites?has_content>
        <tbody>
          <#list nonFavorites as child>
            <#include "listing-tbody.ftl">
          </#list>
        </tbody>
      </#if>
    <#else>
      <#--
        If the user is not the owner of this directory resource, then we render
        the child list unsorted.
      -->
      <tbody>
        <#list allChildren as child>
          <#include "listing-tbody.ftl">
        </#list>
      </tbody>
    </#if>
  <#else>
    <#-- Empty directory resource, nothing to see here. -->
    <tbody>
      <tr>
        <td><i class="far fa-smile-wink"></i> Move along, nothing to see here.</td>
      </tr>
    </tbody>
  </#if>

</table>