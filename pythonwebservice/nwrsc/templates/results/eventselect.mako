<%inherit file="/base.mako" />

<style>
h2 { text-align: left; }
ul { list-style: none; }
</style>

<h2>Event Results:</h2>
<ul>
%for e in c.events:
	<li><a href='${h.url_for(eventid=e.id, action="post")}'>${e.name}</a></li>
%endfor
<li><a href='${h.url_for(eventid=e.id, action="champ")}'>Championship</a></li>
</ul>


<h2>Details:</h2>
<ul>
%for e in c.events:
	<li><a href='${h.url_for(eventid=e.id)}'>${e.name}</a></li>
%endfor
</ul>

