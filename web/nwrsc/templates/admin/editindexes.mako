<%inherit file="base.mako" />
<style>
.repeatedtrfieldset td { text-align: center; }
</style>
<h3>Index Editor</h3>
<% from nwrsc.forms import indexEditForm %>
${indexEditForm(action=h.url_for(action=c.action), value=c.indexlist)|n}
