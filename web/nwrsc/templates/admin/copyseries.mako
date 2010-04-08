<%inherit file="base.mako" />
<h3>Create New Series</h3>
<% from nwrsc.forms import seriesCopyForm %>
${seriesCopyForm(action=h.url_for(action=c.action))|n}

