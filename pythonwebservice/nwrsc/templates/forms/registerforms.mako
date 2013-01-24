<%def name="registerforms()">
<%namespace file="../register/displays.mako" import="carDisplay"/>

<form id='registereventform' method='post' class='hidden'>
<input name='carid' type='hidden'/>
<ul class='selectableevents'>
</ul>
</form>

<form id='registercarform' method='post' class='hidden'>
<input name='eventid' type='hidden'/>
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

