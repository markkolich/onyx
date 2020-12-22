<!-- Footer -->
<footer class="sticky-footer">
<div class="container my-auto">
  <div class="copyright text-center my-auto">
    <p class="mb-2">Crafted with <i class="fa fa-heart text-danger" aria-hidden="true"></i> by <a href="https://mark.koli.ch">Mark S. Kolich</a>.</p>
    <p>Onyx is free &amp; open-source on <a href="https://github.com/markkolich/onyx">GitHub <i class="fab fa-github"></i></a>.</p>
    <#if devMode>
        <code class="text-gray-500">[dev mode]</code>
    <#else>
        <code class="text-gray-500">${buildVersion.getBuildNumber()} &ndash; ${buildVersion.getTimestamp()}</code>
    </#if>
  </div>
</div>
</footer>
<!-- End of Footer -->