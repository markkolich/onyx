<table class="table table-striped table-hover mb-0">

  <#if children?has_content>
      <thead>
          <tr>
              <td class="align-middle">
                  <#if resource.getVisibility() == "PRIVATE">
                      <a href="${contextPath}/<#if view == "browse">details<#else>browse</#if>${resource.getPath()}"><i class="pl-2 fas fa-folder-open text-body"></i></a>
                  <#else>
                      <a href="${contextPath}/<#if view == "browse">details<#else>browse</#if>${resource.getPath()}"><i class="pl-2 far fa-folder-open text-body"></i></a>
                  </#if>
              </td>
              <td class="align-middle" colspan="4">
                  <span>${resource.getHtmlName()}</span>
                  <#if (directoryCount+fileCount > 0)>
                      <p class="text-muted small mb-0">
                          <#if (directoryCount > 0)>
                              ${directoryCount} <#if (directoryCount > 1)>directories<#else>directory</#if>
                              <#if (fileCount > 0)>,&nbsp;</#if>
                          </#if>
                          <#if (fileCount > 0)>
                              ${fileCount} file<#if (fileCount > 1)>s</#if> (${totalFileDisplaySize})
                          </#if>
                      </p>
                  </#if>
              </td>
          </tr>
      </thead>
  </#if>

  <tbody>
      <#if children?has_content>
          <#list children as child>
              <tr data-resource="${child.getPath()}" data-resource-visibility="${child.getVisibility()}" data-resource-favorite="${child.getFavorite()?then('true','false')}">
                  <td class="align-middle">

                      <#if child.getType() == "DIRECTORY">
                          <#if child.getVisibility() == "PRIVATE">
                              <a href="${contextPath}/details${child.getPath()}"><i class="pl-2 fas fa-folder text-body"></i></a>
                          <#else>
                              <a href="${contextPath}/details${child.getPath()}"><i class="pl-2 far fa-folder text-body"></i></a>
                          </#if>
                      <#elseif child.getType() == "FILE">
                          <#if child.getVisibility() == "PRIVATE">
                              <a href="${contextPath}/details${child.getPath()}"><i class="pl-2 fas fa-file text-body"></i></a>
                          <#else>
                              <a href="${contextPath}/details${child.getPath()}"><i class="pl-2 far fa-file text-body"></i></a>
                          </#if>
                      </#if>

                  </td>
                  <td class="align-middle">
                      <#if child.getType() == "DIRECTORY">
                          <a href="${contextPath}/<#if view == "browse">browse<#else>details</#if>${child.getPath()}" data-resource-type="DIRECTORY">${child.getHtmlName()?truncate(40, '...')}</a>
                      <#elseif child.getType() == "FILE">
                          <a href="${contextPath}/file${child.getPath()}" data-resource-type="FILE">${child.getHtmlName()?truncate(40, '...')}</a>
                      </#if>
                  </td>
                  <td class="align-middle d-none d-lg-table-cell">
                      <#if child.getHtmlDescription()?has_content>
                          ${child.getHtmlDescription()?truncate(40, '...')}
                      <#else>
                          <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
                      </#if>
                  </td>
                  <td class="align-middle d-none d-lg-table-cell text-nowrap">
                      <#if child.getType() == "FILE">
                          ${child.getHtmlSize()}
                      <#else>
                          <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
                      </#if>
                  </td>

                  <td class="align-middle text-right">
                      <#if session?has_content>
                          <div class="btn-group" role="group">
                              <#-- Controls are only visible if the authenticated user is the owner of the resource. -->
                              <#if child.getOwner() == session.getUsername()>
                                  <#if child.getType() == "DIRECTORY">
                                      <button type="button" class="btn btn-sm <#if child.getVisibility() == "PUBLIC">btn-dark<#else>btn-light</#if>" data-action="toggle-directory-visibility"><i class="<#if child.getVisibility() == "PUBLIC">fas fa-user-secret<#else>far fa-eye</#if> fa-fw"></i></button>
                                      <button type="button" class="btn btn-sm btn-warning" data-action="toggle-directory-favorite"><i class="<#if child.getFavorite()>fas<#else>far</#if> fa-star fa-fw"></i></button>
                                      <button type="button" class="btn btn-sm btn-danger" data-action="delete-directory" <#if child.getFavorite()>disabled="disabled"</#if>><i class="fas fa-trash fa-fw"></i></button>
                                  <#elseif child.getType() == "FILE">
                                      <button type="button" class="btn btn-sm <#if child.getVisibility() == "PUBLIC">btn-dark<#else>btn-light</#if>" data-action="toggle-file-visibility"><i class="<#if child.getVisibility() == "PUBLIC">fas fa-user-secret<#else>far fa-eye</#if> fa-fw" fa-fw"></i></button>
                                      <button type="button" class="btn btn-sm btn-warning" data-action="toggle-file-favorite"><i class="<#if child.getFavorite()>fas<#else>far</#if> fa-star fa-fw"></i></button>
                                      <button type="button" class="btn btn-sm btn-danger" data-action="delete-file" <#if child.getFavorite()>disabled="disabled"</#if>><i class="fas fa-trash fa-fw"></i></button>
                                  </#if>
                              </#if>
                          </div>
                      <#else>
                          <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
                      </#if>
                  </td>
              </tr>
          </#list>
      <#else>
          <tr>
              <td><i class="far fa-smile-wink"></i> Move along, nothing to see here.</td>
          </tr>
      </#if>
  </tbody>

</table>