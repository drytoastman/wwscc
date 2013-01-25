
<%def name="eventregisterform()">

<form id='registereventform' method='post' class='hidden'>
<input name='carid' type='hidden'/>
<span class='statuslabel'></span>
<ul class='selectableevents'>
%for event in c.events:
	<li data-eventid=${event.id}>
	<input type='checkbox' id='eventreg${event.id}' name='${event.id}' />
	<label for='eventreg${event.id}'>
	${event.name}
	</label>
	</li>
%endfor
</ul>
</form>

</%def>


<%namespace file="../register/displays.mako" import="carDisplay"/>

<%def name="carregisterform()">

<form id='registercarform' method='post' class='hidden'>
<input name='eventid' type='hidden'/>
<span class='statuslabel'></span>
<ul class='selectablecars'>
%for car in c.cars:
	<li data-carid=${car.id}>
	<input type='checkbox' id='carreg${car.id}' name='${car.id}' />
	<label for='carreg${car.id}'>
	${carDisplay(car)}
	</label>
	</li>
%endfor
</ul>
</form>

</%def>

