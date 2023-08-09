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
          <a href="${contextPath}/<#if view == "browse">browse<#else>details</#if>${child.getPath()}" data-resource-type="DIRECTORY">${child.getHtmlName()?truncate(70, '...')}</a>
      <#elseif child.getType() == "FILE">
          <a href="${contextPath}/file${child.getPath()}" data-resource-type="FILE">${child.getHtmlName()?truncate(70, '...')}</a>
      </#if>
  </td>
  <td class="align-middle d-none d-lg-table-cell">
      <#if child.getHtmlDescription()?has_content>
          <span title="${child.getHtmlDescription()}" data-clipboard-text="${child.getHtmlDescription()}">${child.getHtmlDescription()?truncate(70, '...')}</span>
      <#else>
          <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
      </#if>
  </td>
  <td class="align-middle d-none d-lg-table-cell text-nowrap text-gray-500">
      <#if child.getType() == "FILE">
          ${child.getHtmlSize()}
      <#else>
          <#--
            Only show the user the total size of the directory and all
            of its children if the authenticated user is the owner.
          -->
          <#if userIsOwner>
              ${child.getHtmlSize()}
          <#else>
            <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
          </#if>
      </#if>
  </td>

  <td class="align-middle text-right">
      <#if session?has_content>
          <div class="btn-group" role="group">
              <#-- Controls are only visible if the authenticated user is the owner of the resource. -->
              <#if child.getOwner() == session.getUsername()>
                  <#if child.getType() == "DIRECTORY">
                      <button type="button" class="btn btn-sm <#if child.getVisibility() == "PUBLIC">btn-dark<#else>btn-light</#if>" data-action="toggle-directory-visibility"><i class="<#if child.getVisibility() == "PUBLIC">fas fa-user-secret<#else>far fa-eye</#if> fa-fw"></i></button>
                      <button type="button" class="btn btn-sm btn-warning" data-action="toggle-directory-favorite"><i class="<#if child.getFavorite()>fas<#else>far</#if> fa-heart fa-fw"></i></button>
                      <button type="button" class="btn btn-sm btn-danger" data-action="delete-directory" <#if child.getFavorite()>disabled="disabled"</#if>><i class="fas fa-trash fa-fw"></i></button>
                  <#elseif child.getType() == "FILE">
                      <button type="button" class="btn btn-sm <#if child.getVisibility() == "PUBLIC">btn-dark<#else>btn-light</#if>" data-action="toggle-file-visibility"><i class="<#if child.getVisibility() == "PUBLIC">fas fa-user-secret<#else>far fa-eye</#if> fa-fw" fa-fw"></i></button>
                      <button type="button" class="btn btn-sm btn-warning" data-action="toggle-file-favorite"><i class="<#if child.getFavorite()>fas<#else>far</#if> fa-heart fa-fw"></i></button>
                      <button type="button" class="btn btn-sm btn-danger" data-action="delete-file" <#if child.getFavorite()>disabled="disabled"</#if>><i class="fas fa-trash fa-fw"></i></button>
                  </#if>
              </#if>
          </div>
      <#else>
          <div class="mt-2 invisible">&nbsp;</div> <#-- vertical spacer! -->
      </#if>
  </td>
</tr>