<%def name="carlist()">

<ul class='carlist'>
%for car in c.cars:
	<li  class="ui-widget-content">
	<input type='button' value='M' onclick="modify();" ${car.inuse and "disabled"} />
	<input type='button' value='D' onclick="del();" ${car.inuse and "disabled"} />
	${car.number}/${car.classcode} ${car.indexcode and "(%s)" % car.indexcode} ${car.year} ${car.make} ${car.model} ${car.color}
	</li>
%endfor
</ul>

<input class='addnew' type='button' name='create' value='Create New Car' onclick='newcar();'/>

<style>
	.carlist .ui-selecting { background: #FECA40; }
	.carlist .ui-selected { background: #F39814; color: white; }
	.carlist { list-style-type: none; margin: 0; padding: 0; }
	.carlist li { margin: 3px; padding: 0.4em; font-size: 1.4em; height: 18px; }
</style>

</%def>

