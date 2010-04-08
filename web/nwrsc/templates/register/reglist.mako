<%inherit file="base.mako" />
<% current = '' %>

<h2 align=center>${c.event.name} - ${c.event.count} Entries</h2>
<table class='carlist'>
%for driver,car,reg in c.reglist:
%if car.classcode != current:
	<tr><th colspan='4'>${car.classcode} - ${c.classdata.classlist[car.classcode].descrip}</th></tr>
	<% current = car.classcode; counter = 0 %>
%endif

<% counter += 1 %>
<tr>
<td class='counter'>${counter}</td>
<td class='number'>${car.number}</td>
<td>${car.year} ${car.make} ${car.model} ${car.color} ${car.indexcode and "(%s)"%car.indexcode}</td>
<td>${driver.firstname} ${driver.lastname}</td>
</tr>

%endfor
</table>
