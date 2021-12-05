<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <#if children?has_content>
        <#list children as child>
            <url>
                <loc>${fullUri}/browse${child.getPath()}</loc>
            </url>
        </#list>
    </#if>
</urlset>