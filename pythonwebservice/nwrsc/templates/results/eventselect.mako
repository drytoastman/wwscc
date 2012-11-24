<%inherit file="/base.mako" />

<h2>Select an Event:</h2>
<ol>
%for e in c.events:
	<li><a href='${h.url_for(eventid=e.id)}'>${e.name}</a></li>
%endfor
</ol>

