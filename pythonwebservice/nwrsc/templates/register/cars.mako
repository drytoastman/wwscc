<%namespace file="displays.mako" import="carDisplay"/>
<%namespace file="/forms/registerforms.mako" import="carregisterform"/>

<%def name="disablecar(car)" filter="oneline">
%if len(car.regevents) > 0:
disabled='disabled' title='Cars registered or in use cannot be edited or deleted' \
%endif
</%def>

<%def name="disableevent(event)" filter="oneline">
%if not event.isOpen:
disabled='disabled' title='Event registration is not open or car has runs so this assignment cannot be changed' \
%endif
</%def>

<%def name="carlist()">

<table id='carlist'>
<tr><th></th><th></th><th><span>Car</span></th><th><span>Registered/Used In</span></th></tr>
%for car in c.cars:
	<tr>
	<td><button class='editcar' data-driverid='${c.driverid}' data-carid='${car.id}' ${disablecar(car)} >Edit</button></td>
	<td><button class='deletecar' data-carid='${car.id}' ${disablecar(car)}>Delete</button></td>
	<td class='car'>${carDisplay(car)}</td>
	<td class='carevents'>
		%if len(car.regevents) > 0:
		<ul>
		%for (event, regid) in car.regevents:
			<li>
			<button class='unregbutton' data-eventid='${event.id}' data-regid='${regid}' ${disableevent(event)}>Unregister</button>
			<span class='regevent'>${event.name}</span>
			</li>
		%endfor
		</ul>
		%endif

		%if len(car.canregevents) > 0:
		<button class='regbutton' data-carid='${car.id}'>Register For Events</button>
		%else:
		<span class='limit'>There are no more events that this car could be registered in</span>
		%endif
	</td>
	</tr>
%endfor
</table>

<button class='createcar' data-driverid='${c.driverid}'>Create New Car</button>

<%doc> registration forms are recreated  when a new car is created, edited or deleted </%doc>
${carregisterform()}

<script type='text/javascript'>
var cars = {
%for car in c.cars:
${car.id}: ${h.encodesqlobj(car)|n},
%endfor
}
var seriesevents = {
%for event in c.events:
 ${event.id}: { id: ${event.id}, name: "${event.name}", doublespecial: ${event.doublespecial and "true" or "false"}, \
			totlimit: ${event.totlimit or 9999}, perlimit: ${event.perlimit or 9999}, count: ${event.count}, drivercount: ${event.drivercount} },
%endfor
}

carTabSetup(); 
</script>

</%def>

