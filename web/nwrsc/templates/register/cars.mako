<%def name="carlist()">

<ul id='carlist'>
%for car in c.cars:
	<li>
	<input type='button' value='Edit' onclick="editcar(${c.driver.id}, ${car.id});" ${car.inuse and "disabled"} />
	<input type='button' value='Delete' onclick="deletecar(${car.id});" ${car.inuse and "disabled"} />
	<span class='carclass'>${car.classcode}/${car.number}</span>
	<span class='carindex'>${car.indexcode and "(%s)" % car.indexcode}</span>
	<span class='cardesc'>${car.year} ${car.make} ${car.model} ${car.color}</span>
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

