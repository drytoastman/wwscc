<%inherit file="/admin/base.mako" />

<% def singleline(text): return text.replace('\n', '') %>

<%def name="dorow(ii, field)" filter="singleline">
<tr>
<td><input type="text" name="extraslist-${ii}.name" value="${field.name}" size="10" /></td>
<td><input type="text" name="extraslist-${ii}.type" value="${field.type}" size="6" /></td>
<td><button class="small deleterow">Del</button></td>
</tr>
</%def>

<h3>Driver Fields Editor</h3>

<p>
Any extra fields in this list will be available as extra fields for each driver entry.

<table>
<tr><th>Name</th><td>The field name</td></tr>
<tr><th>Type</th><td>The field type</td></tr>
</table>

</p>

<p></p>


<form action="${c.action}" method="post">
<table id='extrastable'>
<tr>
<th>Name</th>
<th>Type</th>
</tr>

<% ii = 0 %>
%for ii, field in enumerate(c.fieldlist):
${dorow(ii, field)}
%endfor

</table>
<button id='addbutton'>Add</button>
<input type='submit' value="Save">
</form>

<% from nwrsc.model import DriverField %>
<script>
var rowstr = '${dorow('xxxxx', DriverField())}\n';
var ii = ${ii};
$('#addbutton').click(function() {
	$("#extrastable > tbody").append(rowstr.replace(/xxxxx/g, ++ii));
	$('#extrastable button').button();
	return false;
});
</script>

