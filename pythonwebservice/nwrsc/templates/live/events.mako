<%inherit file="base.mako" />

<div data-role="page" data-theme="b">

	<div data-role="header">
		<h1>Select Series</h2>
	</div> 

	<div data-role="content">
%for event in sorted(c.events, key=lambda x:x.date):
	<a href='${h.url_for(eventid=event.id)}/' data-role='button' rel="external">${event.name}</a>
%endfor
	</div> 

</div>
	

