<%def name="driverform(dr, action)">

<style>
#drivereditor input { width: 100%; }
th { padding-left: 14px; padding-right: 5px; }
th { text-align: right; }
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
<th>Email</th>
<td colspan='6'><input name='email' type='text' value='${dr.email}'/></td>
</tr>

<tr>
<th>Membership</th>
<td colspan='6'><input name='membership' type='text' value='${dr.membership}'/></td>
</tr>

<tr>
<th>Address</th>
<td colspan='6'><input name='address' type='text' value='${dr.address}'/></td>
</tr>

<tr>
<th>City</th>
<td colspan='2'><input name='city' type='text' value='${dr.city}'/></td>
<th>State</th>
<td><input name='state' type='text' value='${dr.state}' size=2/></td>
<th>Zip</th>
<td><input name='zip' type='text' value='${dr.zip}' size=2/></td>
</tr>

<tr>
<th>Home Phone</th>
<td colspan='6'><input name='homephone' type='text' value='${dr.homephone}'/></td>
</tr>

<tr>
<th>Brag</th>
<td colspan='6'><input name='brag' type='text' value='${dr.brag}'/></td>
</tr>

<tr>
<th>Sponsor</th>
<td colspan='6'><input name='sponsor' type='text' value='${dr.sponsor}'/></td>
</tr>

<tr>
<th>Clubs</th>
<td colspan='6'><input name='clubs' type='text' value='${dr.clubs}'/></td>
</tr>

</tbody>
</table>
</form>

</%def>

