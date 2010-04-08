<%inherit file="base.mako" />
<h3>Event Editor</h3>
<% from nwrsc.forms import eventForm %>
${eventForm(action=h.url_for(action=c.action), value=c.event)|n}
