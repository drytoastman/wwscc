<%inherit file="base.mako" />

<h2>${c.event.name} Entrants</h2>

<form method='post' action='${h.url_for(action='delreg')}'>
<input type='hidden' name='regid' value='-1'/>
<table id='xregtable'>
<thead>
<tr>
<th>Id</th>
<th>Class</th>
<th>#</th>
<th>First</th>
<th>Last</th>
<th>Email</th>
<th>Car</th>
<th></th>
</tr>
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
<td>${e.driver.email}</td>
<td>${e.car.make} ${e.car.model} ${e.car.color}</td>
<td><input type='submit' value='unreg' onClick='this.form.regid.value=${e.reg.id};' /></td>
</tr>

%endfor
%endfor

</tbody>
</table>
</form>

<script type='text/javascript'>
$(document).ready(function(){ 
	$('input').button();
	$('#xregtable').dataTable({
		"bJQueryUI" : true,
		"bPaginate": false
	});
});
</script>

