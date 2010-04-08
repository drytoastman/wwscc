<%inherit file="base.mako" />
<h2>${c.event.name} Entrants</h2>
Click on column header to change sort.
<P/>
<form method='post' action='${h.url_for(action='delreg')}'>
<input type='hidden' name='regid' value='-1'>
<table class='reglist sortable'>
<thead>
<tr>
<th>Id</th>
<th>Class</th>
<th>#</th>
<th class='sorttable_ncalpha'>First</th>
<th class='sorttable_ncalpha'>Last</th>
<th class='sortable_ncalpha'>Member #</th>
<th class='sortable_ncalpha'>Email</th>
<th>Car</th>
<th class='sorttable_nosort'></th>
</thead>
<tbody>

%for cls in sorted(c.registered):
%for e in sorted(c.registered[cls], key=lambda obj: obj.car.number):

<tr>
<td>${e.reg.id}</td>
<td>${e.car.classcode}</td>
<td>${e.car.number}</td>
<td>${e.driver.firstname}</td>
<td>${e.driver.lastname}</td>
<td>${e.driver.membership}</td>
<td>${e.driver.email}</td>
<td>${e.car.make} ${e.car.model} ${e.car.color}</td>
<td><input type='submit' value='unreg' onClick='this.form.regid.value=${e.reg.id};'></td>
<%doc><td><a href='${h.url_for(action='print', carid=e.car.id)}'>Card</a></td></%doc>
</tr>

%endfor
%endfor

</tbody>
</table>
</form>

