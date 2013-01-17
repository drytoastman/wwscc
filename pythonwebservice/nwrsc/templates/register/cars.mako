<%namespace file="displays.mako" import="carDisplay"/>

<%def name="disablewarn()">
disabled='disabled' title='Cars registered or in use cannot be edited or deleted'
</%def>


<%def name="carlist()">

<table id='carlist'>
%for car in c.cars:
	<%
		regevents = [reg.
	<tr>
	<td><button class='edit' onclick="editcar(${c.driver.id}, ${car.id});" ${car.inuse and disablewarn()} ></button></td>
	<td><button class='delete' onclick="deletecar(${car.id});" ${car.inuse and disablewarn()}></button></td>
	<td class='car'>${carDisplay(car)}</span>
	</tr>
%endfor
</table>

<script type='text/javascript'>
var cars = new Array();
%for car in c.cars:
cars[${car.id}] = ${h.encodesqlobj(car)|n}
%endfor
$("#carlist .delete").button({icons: { primary:'ui-icon-scissors'}, text: false} );
$("#carlist .edit").button({icons: { primary:'ui-icon-pencil'}, text: false} );
</script>

</%def>

