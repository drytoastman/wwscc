<%def name="driverform(dr, action)">

<style>
#drivereditor input { width: 100%; background: #fffffe; }
.drivereditor th { padding-left: 14px; padding-right: 5px; }
.drivereditor th { text-align: right; }
.drivereditor td { text-align: right; }
</style>

<form id='drivereditor' action='${action}' method='POST'>
<input name='driverid' type='hidden' value='${dr.id}'/>
<table class='drivereditor'>
<tbody>

<tr>
<th>First Name</th>
<td colspan='2'><input name='firstname' type='text' value='${dr.firstname}'/></td>
<th>Last Name</th>
<td colspan='3'><input name='lastname' type='text' value='${dr.lastname}'/></td>
</tr>

<tr>
<th>Email/UniqueId</th>
<td colspan='6'><input name='email' type='text' value='${dr.email}'/></td>
</tr>

<tr>
<td>Address</td>
<td colspan='6'><input name='address' type='text' value='${dr.address}'/></td>
</tr>

<tr>
<td>City</td>
<td colspan='2'><input name='city' type='text' value='${dr.city}'/></td>
<td>State</td>
<td><input name='state' type='text' value='${dr.state}' size=2/></td>
<td>Zip</td>
<td><input name='zip' type='text' value='${dr.zip}' size=2/></td>
</tr>

<tr>
<td>Phone</td>
<td colspan='6'><input name='phone' type='text' value='${dr.phone}'/></td>
</tr>

<tr>
<td>Brag</td>
<td colspan='6'><input name='brag' type='text' value='${dr.brag}'/></td>
</tr>

<tr>
<td>Sponsor</td>
<td colspan='6'><input name='sponsor' type='text' value='${dr.sponsor}'/></td>
</tr>

%for field in c.fields:
<tr>
<td>${field.title}</td>
<td colspan='6'><input name='${field.name}' type='text' value='${dr.getExtra(field.name)}'/></td>
</tr>
%endfor

</tbody>
</table>
</form>

</%def>

