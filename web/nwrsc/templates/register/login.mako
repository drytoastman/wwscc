<%inherit file="base.mako" />
<% from nwrsc.forms import loginForm %>
<h2 style='margin-left:20px'>${c.seriesname}</h2>
${loginForm(action=h.url_for(action='checklogin'))|n}
