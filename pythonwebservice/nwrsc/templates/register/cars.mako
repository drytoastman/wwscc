<%namespace file="displays.mako" import="carDisplay"/>

<%def name="disablecar(car)">
%if len(car.regevents) > 0:
disabled='disabled' title='Cars registered or in use cannot be edited or deleted'
%endif
</%def>

<%def name="disableevent(event)">
%if not event.opened or event.closed:
disabled='disabled' title='Event registration is not open or car has runs so this assignment cannot be changed'
%endif
</%def>

<%def name="carlist()">

<table id='carlist'>
<tr><th></th><th></th><th><span>Car</span></th><th><span>Registered/Used In</span></th></tr>
%for car in c.cars:
	<tr>
	<td><button class='edit' onclick="editcar(${c.driverid}, ${car.id});" ${disablecar(car)} >Edit</button></td>
	<td><button class='delete' onclick="deletecar(${car.id});" ${disablecar(car)}>Delete</button></td>
	<td class='car'>${carDisplay(car)}</td>
	<td class='carevents'>
		<ul>
		%for (event, regid) in car.regevents:
			<li>
			<button class='unregbutton' data-eventid="${event.id}" data-regid=${regid}' ${disableevent(event)}>Unregister</button>
			<span class='regevent'>${event.name}</span>
			</li>
		%endfor
		</ul>
		<button class='regbutton' onclick='$("#registereventform").registerForEventDialog(${car.id}, ${car.canregevents}, finishregedit);'>Register For Events</button>
	</td>
	</tr>
%endfor
</table>

<script type='text/javascript'>
var cars = new Array();
var cevents = new Array();
%for car in c.cars:
cars[${car.id}] = ${h.encodesqlobj(car)|n}
%endfor
%for event in c.eventmap.values():
cevents[${event.id}] = ${h.encodesqlobj(event)|n}
%endfor
$("#carlist .delete").button({icons: { primary:'ui-icon-trash'}, text: false} );
$("#carlist .edit").button({icons: { primary:'ui-icon-pencil'}, text: false} );

$("#carlist .regbutton").button().click() { function() {
	$("#registereventform").registerForEventDialog($(this).data('carid'), $(this).data('openevents'), function() {
		$('#carswrapper').nwr('loadCars');
		$('#registereventform input:checked').each(function() { 
			var eventid = $(this).prop('name');
			$('#event'+eventid).nwr('loadEvent', eventid);
		});
	});
}
});

$("#carlist .unregbutton").button({icons: { primary:'ui-icon-scissors'}, text: false} ).click(function () {
	unregisterCar(this, $(this).data('event'), $(this).data('reg'));
});
</script>

</%def>

