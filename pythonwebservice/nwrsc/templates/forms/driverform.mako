<%def name="driverform(action='', method='post', driver=None, allowalias=False)">

<%
	from nwrsc.model import Driver
	if driver is None:
		driver = Driver()
%>

<form id='drivereditor' action='${action}' method='${method}'>
<div id='driverhelp'>First Name, Last Name and Unique/Email require at least 2 characters each</div>
<input name='driverid' type='hidden' value='-1'/>
<table> <tbody>

<tr id='namerow'>
<th>First Name</th>
<td colspan='2'><input name='firstname' type='text' value='${driver.firstname}'/></td>
<th>Last Name</th>
<td colspan='3'><input name='lastname' type='text' value='${driver.lastname}'/></td>
</tr>

<tr id='emailrow'>
<th>Email/UniqueId</th>
<td colspan='6'><input name='email' type='text' value='${driver.email}'/></td>
</tr>

<tr id='aliasrow'>
<th>Public Alias</th>
<td colspan='6'>
%if allowalias:
<input name="alias" type='text' value='${driver.alias}'>
%else:
<span id="aliasspan">${driver.alias}</span>
%endif
</td>
</tr>

<tr>
<th>Address</th>
<td colspan='6'><input name='address' type='text' value='${driver.address}'/></td>
</tr>

<tr>
<th>City</th>
<td colspan='2'><input name='city' type='text' value='${driver.city}'/></td>
<th>State</th>
<td><input name='state' type='text' value='${driver.state}' size="2"/></td>
<th>Zip</th>
<td><input name='zip' type='text' value='${driver.zip}' size="2"/></td>
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

