<%namespace file="displays.mako" import="carDisplay"/>

<%def name="disablecar(car)" filter="oneline">
%if len(car.regevents) > 0:
disabled='disabled' title='Cars registered or in use cannot be edited or deleted' \
%endif
</%def>

<%def name="disableevent(event)" filter="oneline">
%if not event.opened or event.closed:
disabled='disabled' title='Event registration is not open or car has runs so this assignment cannot be changed' \
%endif
</%def>

<%def name="carlist()">

<table id='carlist'>
<tr><th></th><th></th><th><span>Car</span></th><th><span>Registered/Used In</span></th></tr>
%for car in c.cars:
	<tr>
	<td><button class='edit' data-driverid='${c.driverid}' data-carid='${car.id}' ${disablecar(car)} >Edit</button></td>
	<td><button class='delete' data-carid='${car.id}' ${disablecar(car)}>Delete</button></td>
	<td class='car'>${carDisplay(car)}</td>
	<td class='carevents'>
		<ul>
		%for (event, regid) in car.regevents:
			<li>
			<button class='unregbutton' data-eventid='${event.id}' data-regid='${regid}' ${disableevent(event)}>Unregister</button>
			<span class='regevent'>${event.name}</span>
			</li>
		%endfor
		</ul>
		<button class='regbutton' data-carid='${car.id}'>Register For Events</button>
	</td>
	</tr>
%endfor
</table>

<script type='text/javascript'>
var cars = {
%for car in c.cars:
${car.id}: ${h.encodesqlobj(car)|n},
%endfor
}
var eventnames = {
%for event in c.eventmap.values():
 ${event.id}: "${event.name}",
%endfor
}

carTabSetup();
</script>

</%def>

