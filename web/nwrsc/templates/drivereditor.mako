<%def name="drivereditor()">

<script>
function editdriver(did)
{
	$('#driverid').val(did);
	$('#firstname').val(drivers[did].firstname);
	$('#lastname').val(drivers[did].lastname);
	$('#email').val(drivers[did].email);
	$('#membership').val(drivers[did].membership);
	$('#address').val(drivers[did].address);
	$('#city').val(drivers[did].city);
	$('#state').val(drivers[did].state);
	$('#zip').val(drivers[did].zip);
	$('#homephone').val(drivers[did].homephone);
	$('#brag').val(drivers[did].brag);
	$('#sponsor').val(drivers[did].sponsor);
	$('#clubs').val(drivers[did].clubs);
	$('#drivereditor').dialog('open');
}

$(document).ready(function(){
    $("#drivereditor").validate();

	$("#drivereditor").dialog({
		autoOpen: false,
		height: 350,
		width: 550,
		modal: true,
		title: 'Driver Editor',
		buttons: {
			'Ok': function() {
				if ($("#drivereditor").valid()) {
					$(this).dialog('close');
					driveredited();
				}
			},
			Cancel: function() { $(this).dialog('close'); }
		},
		close: function() {
		}
	});
});
</script>

<style>
.drivereditor input { width: 100%; }
</style>


<form id='drivereditor' action=''>
<input id='driverid' name='driverid' type='hidden'/>
<table class='drivereditor'>
<tbody>

<tr>
<th>Name</th>
<td><input id='firstname' name='firstname' type='text'/></td><td colspan='2'><input id='lastname' name='lastname' type='text'/></td>
</tr>

<tr>
<th>Email</th>
<td colspan='3'><input id='email' name='email' type='text'/></td>
</tr>

<tr>
<th>Membership</th>
<td colspan='3'><input id='membership'   name='membership'   type='text'/></td>
</tr>

<tr>
<th>Address</th>
<td colspan='3'><input id='address'   name='address'   type='text'/></td>
</tr>

<tr>
<th>CSZ</th>
<td><input id='city' name='city' type='text'/></td>
<td><input id='state' name='state' type='text'/></td>
<td><input id='zip' name='zip' type='text'/></td>
</tr>

<tr>
<th>Home Phone</th>
<td colspan='3'><input id='homephone' name='homephone' type='text'/></td>
</tr>

<tr>
<th>Brag</th>
<td colspan='3'><input id='brag' name='brag' type='text'/></td>
</tr>

<tr>
<th>Sponsor</th>
<td colspan='3'><input id='sponsor' name='sponsor' type='text'/></td>
</tr>

<tr>
<th>Clubs</th>
<td colspan='3'><input id='clubs' name='clubs' type='text'/></td>
</tr>

</tbody>
</table>
</form>

</%def>
