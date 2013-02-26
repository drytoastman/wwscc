<%inherit file="base.mako" />
<%namespace file="/forms/carform.mako" import="carform"/>

<%def name="carline(car)">
<li>
<button class='ceditor' onclick='iceditcar(${car.driverid}, ${car.id});'>Edit</button>
<button class='ceditor' onclick='icdeletecar(${car.id});' >Delete</button>
${car.driver.firstname} ${car.driver.lastname} - ${car.classcode} ${h.ixstr(car)} #${car.number} ${car.year} ${car.make} ${car.model} ${car.color} 
</li>
</%def>

<h2>Invalid Numbers (No number)</h2>
<ul class='invalidlist'>
%for car in c.invalidnumber:
	${carline(car)}
%endfor
</ul>

<h2>Invalid Class (No class or not in classlist)</h2>
<ul class='invalidlist'>
%for car in c.invalidclass:
	${carline(car)}
%endfor
</ul>

<h2>Invalid Index (Needs index and is blank or not in indexlist)</h2>
<ul class='invalidlist'>
%for car in c.invalidindex:
	${carline(car)}
%endfor
</ul>

<h2>Restricted Index (Has index in the restricted list)</h2>
<ul class='invalidlist'>
%for car in c.restrictedindex:
	${carline(car)}
%endfor
</ul>


${carform(False)}

<script>
%for car in c.invalidnumber + c.invalidclass + c.invalidindex + c.restrictedindex:
cars[${car.id}] = ${h.encodesqlobj(car)|n} 
%endfor

function iceditcar(did, cid)
{
    $('#careditor').CarEdit('doDialog', did, cars[cid], function() {
        $.nwr.updateCar($("#careditor").serialize(), function() {
			location.reload();
        })
    });

}

function icdeletecar(cid)
{
	$.post($.nwr.url_for('deletecar'), { carid: cid }, function() {
		location.reload();
	});
}

</script>

