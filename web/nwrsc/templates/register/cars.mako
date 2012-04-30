<%def name="disablewarn()">
disabled title='Cars registered or in use cannot be edited or deleted'
</%def>


<%def name="carlist()">

<table id='carlist'>
<tr><th></th><th></th><th>Class</th><th>Num</th><th>Idx</th><th>Description</th></tr>
%for car in c.cars:
	<tr>
	<td><button class='edit' onclick="editcar(${c.driver.id}, ${car.id});" ${car.inuse and disablewarn()} ></button></td>
	<td><button class='delete' onclick="deletecar(${car.id});" ${car.inuse and disablewarn()}></button></td>
	<td class='carclass'>${car.classcode}</td>
	<td class='carnumber'>${car.number}</td>
	<td class='carindex'>${car.indexcode}</td>
	<td class='cardesc'>${car.year} ${car.make} ${car.model} ${car.color}</td>
	</tr>
%endfor
</table>

<script>
cars = Array();
%for car in c.cars:
cars[${car.id}] = ${h.encodesqlobj(car)|n}
%endfor
$("#carlist .delete").button({icons: { primary:'ui-icon-scissors'}, text: false} );
$("#carlist .edit").button({icons: { primary:'ui-icon-pencil'}, text: false} );
</script>

</%def>

