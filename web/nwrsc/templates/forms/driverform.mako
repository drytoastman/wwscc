<%def name="driverform(action=None, method=None)">

<style>
#drivereditor input { width: 100%; }
#drivereditor th { padding-left: 14px; padding-right: 5px; }
#drivereditor th, #drivereditor td { text-align: right; font-weight: normal; }
#namerow th, #emailrow th { font-weight: bold; }
#emailrow th, #emailrow td { padding-bottom: 20px; }
</style>

<%
	addition = ""
	if action is not None:
		addition += "action='%s' " % action
	if method is not None:
		addition += "method='%s' " % method
%>

<form id='drivereditor' ${addition}>
<input name='driverid' type='hidden' value='-1'/>
<table>
<tbody>

<tr id='namerow'>
<th>First Name</th>
<td colspan='2'><input name='firstname' type='text' value=''/></td>
<th>Last Name</th>
<td colspan='3'><input name='lastname' type='text' value=''/></td>
</tr>

<tr id='emailrow'>
<th>Email/UniqueId</th>
<td colspan='6'><input name='email' type='text' value=''/></td>
</tr>

<tr>
<th>Address</th>
<td colspan='6'><input name='address' type='text' value=''/></td>
</tr>

<tr>
<th>City</th>
<td colspan='2'><input name='city' type='text' value=''/></td>
<th>State</th>
<td><input name='state' type='text' value='' size=2/></td>
<th>Zip</th>
<td><input name='zip' type='text' value='' size=2/></td>
</tr>

<tr>
<th>Phone</th>
<td colspan='6'><input name='phone' type='text' value=''/></td>
</tr>

<tr>
<th>Brag</th>
<td colspan='6'><input name='brag' type='text' value=''/></td>
</tr>

<tr>
<th>Sponsor</th>
<td colspan='6'><input name='sponsor' type='text' value=''/></td>
</tr>

%for field in c.fields:
<tr>
<th>${field.title}</th>
<td colspan='6'><input name='${field.name}' type='text' value=''/></td>
</tr>
%endfor

</tbody>
</table>
</form>

</%def>

