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
	datatable = $('#contacttable').dataTable({
		"bJQueryUI" : true,
	});
});
</script>

<h2>Contact List for ${c.title}</h2>

<p>
Use the search box to limit the visible drivers in the table.  This also limits the drivers selected for the CSV download or email copy/paste.
</p>

<form style="display: hidden" action="${h.url_for(action='downloadcontacts')}" method="POST" id="contactform">
<input type='hidden' name='ids' value=''/>
<input type='Submit' name='Submit' value='Download Full CSV List' onclick='return downloadfull();'/>
<input type='Submit' name='Export' value='Copy Valid Email' onclick='return copyemail();'/>
</form>
<br/>

<table id='contacttable'>
<thead>
<tr><th>Id</th><th>First</th><th>Last</th><th>Email</th><th>Classes</th>
%if c.showevents:
<th>Events</th>
%endif
</tr>
</thead>
<tbody>
%for dr in c.drivers.values():
<tr><td>${dr.id}</td><td>${dr.firstname}</td><td>${dr.lastname}</td><td>${dr.email}</td><td>${','.join(dr.classes)}</td>
%if c.showevents:
<td>${','.join(["e%02d" % x for x in dr.events])}</td>
%endif
</tr>
%endfor
</tbody>
</table>

