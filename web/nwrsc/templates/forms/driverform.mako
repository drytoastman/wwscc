<%def name="driverform(action=None, method='POST', driver=None)">

<style>
#drivereditor input { width: 100%; }
#drivereditor th { padding-left: 14px; padding-right: 5px; }
#drivereditor th, #drivereditor td { text-align: right; font-weight: normal; }
#namerow th, #emailrow th, #anonrow th { font-weight: bold; }
#emailrow th, #emailrow td { padding-bottom: 20px; }
#anonrow input { width: auto; }
#anonrow td { text-align: left; }
</style>

<%
	from nwrsc.model import Driver
	addition = ""
	if action is not None:
		addition += "action='%s' " % action
	if method is not None:
		addition += "method='%s' " % method
	if driver is None:
		driver = Driver()
%>

<form id='drivereditor' ${addition}>
<input name='driverid' type='hidden' value='-1'/>
<table>
<tbody>

<tr id='namerow'>
<th>First Name</th>
<td colspan='2'><input name='firstname' type='text' value='${driver.firstname}'/></td>
<th>Last Name</th>
<td colspan='3'><input name='lastname' type='text' value='${driver.lastname}'/></td>
</tr>

<tr id='anonrow'>
<th>Anonymize Results</th>
<td colspan='6'><input type="checkbox" name="anonymize" ${driver.anonymize and "checked"}></td>
</tr>

<tr id='emailrow'>
<th>Email/UniqueId</th>
<td colspan='6'><input name='email' type='text' value='${driver.email}'/></td>
</tr>

<tr>
<th>Address</th>
<td colspan='6'><input name='address' type='text' value='${driver.address}'/></td>
</tr>

<tr>
<th>City</th>
<td colspan='2'><input name='city' type='text' value='${driver.city}'/></td>
<th>State</th>
<td><input name='state' type='text' value='${driver.state}' size=2/></td>
<th>Zip</th>
<td><input name='zip' type='text' value='${driver.zip}' size=2/></td>
</tr>

<tr>
<th>Phone</th>
<td colspan='6'><input name='phone' type='text' value='${driver.phone}'/></td>
</tr>

<tr>
<th>Brag</th>
<td colspan='6'><input name='brag' type='text' value='${driver.brag}'/></td>
</tr>

<tr>
<th>Sponsor</th>
<td colspan='6'><input name='sponsor' type='text' value='${driver.sponsor}'/></td>
</tr>

%for field in c.fields:
<tr>
<th>${field.title}</th>
<td colspan='6'><input name='${field.name}' type='text' value='${driver.getExtra(field)}'/></td>
</tr>
%endfor

</tbody>
</table>
</form>

</%def>

