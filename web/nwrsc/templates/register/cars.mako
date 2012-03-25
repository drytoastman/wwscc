<%def name="carlist()">

<ul id='carlist'>
%for car in c.cars:
	<li>
	<input type='button' value='Edit' onclick="editcar(${c.driver.id}, ${car.id});" ${car.inuse and "disabled"} />
	<input type='button' value='Delete' onclick="deletecar(${car.id});" ${car.inuse and "disabled"} />
	${car.number}/${car.classcode} ${car.indexcode and "(%s)" % car.indexcode} ${car.year} ${car.make} ${car.model} ${car.color}
	</li>
%endfor
</ul>

<script>
cars = Array();
%for car in c.cars:
cars[${car.id}] = ${h.encodesqlobj(car)|n}
%endfor
$("#carlist input[type='button']").button();
</script>

</%def>

