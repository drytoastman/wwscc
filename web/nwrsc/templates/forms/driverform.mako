<%def name="driverform()">

<style>
.drivereditor input { width: 100%; }
.drivereditor th { padding-left: 14px; padding-right: 5px; }
.drivereditor th { text-align: right; }
.drivereditor td { text-align: right; }
</style>

<form id='drivereditor'>
<input name='driverid' type='hidden' value='-1'/>
<table class='drivereditor'>
<tbody>

<tr>
<th>First Name</th>
<td colspan='2'><input name='firstname' type='text' value=''/></td>
<th>Last Name</th>
<td colspan='3'><input name='lastname' type='text' value=''/></td>
</tr>

<tr>
<th>Email/UniqueId</th>
<td colspan='6'><input name='email' type='text' value=''/></td>
</tr>

<tr>
<td>Address</td>
<td colspan='6'><input name='address' type='text' value=''/></td>
</tr>

<tr>
<td>City</td>
<td colspan='2'><input name='city' type='text' value=''/></td>
<td>State</td>
<td><input name='state' type='text' value='' size=2/></td>
<td>Zip</td>
<td><input name='zip' type='text' value='' size=2/></td>
</tr>

<tr>
<td>Phone</td>
<td colspan='6'><input name='phone' type='text' value=''/></td>
</tr>

<tr>
<td>Brag</td>
<td colspan='6'><input name='brag' type='text' value=''/></td>
</tr>

<tr>
<td>Sponsor</td>
<td colspan='6'><input name='sponsor' type='text' value=''/></td>
</tr>

%for field in c.fields:
<tr>
<td>${field.title}</td>
<td colspan='6'><input name='${field.name}' type='text' value=''/></td>
</tr>
%endfor

</tbody>
</table>
</form>

</%def>

