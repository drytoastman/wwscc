<%inherit file="base.mako" />
<style>
.repeatedtrfieldset td { text-align: center; }
</style>
<h3>Class Editor</h3>
<% from nwrsc.forms import classEditForm %>
${classEditForm(action=h.url_for(action=c.action), value=c.classlist)|n}
