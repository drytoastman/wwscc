<%inherit file="/base.mako" />

<style>
h2 { text-align: left; }
ul { list-style: none; }
</style>

<h2>Event Details:</h2>
<ul>
%for e in sorted(c.events, key=lambda x: x.date):
	<li><a href='${h.url_for(eventid=e.id)}'>${e.name}</a></li>
%endfor
<li>&nbsp;</li>
<li><a href='${h.url_for(eventid=e.id, action="champ")}'>Championship</a></li>
</ul>

