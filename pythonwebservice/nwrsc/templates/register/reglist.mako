<%inherit file="/base.mako" />
<% current = None; counter = 0 %>

<div style='margin:20px'>
<h2>{{c.event.name}} - {{c.event.count}} Entries ({{c.event.drivercount}} Unique)</h2>
<table class='carlist'>
%for driver,car,reg in c.reglist:
%if car.classcode != current:
	<% 
		cd = c.classdata.classlist.get(car.classcode)
		if cd is None:
			descrip = "Invalid class"
		else:
			descrip = cd.descrip
	%>
	<tr><th colspan='4'>{{car.classcode}} - {{descrip}}</th></tr>
	<% current = car.classcode; counter = 0 %>
%endif

<% counter += 1 %>
<tr>
<td class='counter'>{{counter}}</td>
<td class='number'>{{car.number}}</td>
<td>{{car.year}} {{car.make}} {{car.model}} {{car.color}} {{h.ixstr(car)}}</td>
<td>{{driver.firstname}} {{driver.lastname}}</td>
</tr>

%endfor
</table>

</div>
