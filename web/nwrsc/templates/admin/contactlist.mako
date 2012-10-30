<%inherit file="base.mako" />

<script type="text/javascript"> 
function downloadfull()
{
	rows = datatable._('tr', {"filter":"applied"});
	ids = Array();
	for (var ii = 0; ii < rows.length; ii++) {
		ids.push(rows[ii][0]);
	}
	$("input[name=ids]").attr('value', ids);
}

function copyemail()
{
	rows = datatable._('tr', {"filter":"applied"});
	email = Array();
	for (var ii = 0; ii < rows.length; ii++) {
		if (rows[ii][3].indexOf("@") > 1)  // dumb but still useful filter
			email.push(rows[ii][3]);
	}
	prompt("Copy the follow string to your clipboard", email);
	return false;
}

var datatable;
$(document).ready(function(){ 
	datatable = $('#contacttable').dataTable();
});
</script>

<h2>Contact List</h2>

<form style="display: hidden" action="${h.url_for(action='downloadcontacts')}" method="POST" id="contactform">
<input type='hidden' name='ids' value=''/>
<input type='Submit' name='Submit' value='Download Full CSV List' onclick='return downloadfull();'/>
<input type='Submit' name='Export' value='Copy Valid Email' onclick='return copyemail();'/>
</form>

<table id='contacttable'>
<thead>
<tr><th>Id</th><th>First</th><th>Last</th><th>Email</th><th>Classes</th><th>Events</th></tr>
</thead>
<tbody>
%for dr in c.drivers.values():
<tr><td>${dr.id}</td><td>${dr.firstname}</td><td>${dr.lastname}</td><td>${dr.email}</td><td>${','.join(dr.classes)}</td><td>${','.join(["e%02d" % x for x in dr.events])}</td></tr>
%endfor
</tbody>
</table>

