<%inherit file="base.mako" />
<h3>Create Event</h3>
<% from nwrsc.forms import eventForm %>
${eventForm(action=h.url_for(action=c.action))|n}
