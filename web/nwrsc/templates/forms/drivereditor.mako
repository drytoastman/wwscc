<%def name="drivereditor()">
<%namespace file="driverform.mako" import="driverform" />

<script>
function editdriver(did)
{
	if (did in drivers)
	{
		$('#drivereditor [name=firstname]').val(drivers[did].firstname);
		$('#drivereditor [name=lastname]').val(drivers[did].lastname);
		$('#drivereditor [name=email]').val(drivers[did].email);
		$('#drivereditor [name=membership]').val(drivers[did].membership);
		$('#drivereditor [name=address]').val(drivers[did].address);
		$('#drivereditor [name=city]').val(drivers[did].city);
		$('#drivereditor [name=state]').val(drivers[did].state);
		$('#drivereditor [name=zip]').val(drivers[did].zip);
		$('#drivereditor [name=homephone]').val(drivers[did].homephone);
		$('#drivereditor [name=brag]').val(drivers[did].brag);
		$('#drivereditor [name=sponsor]').val(drivers[did].sponsor);
		$('#drivereditor [name=clubs]').val(drivers[did].clubs);
	}
	else
	{
		$('#drivereditor [name=firstname]').val("");
		$('#drivereditor [name=lastname]').val("");
		$('#drivereditor [name=email]').val("");
		$('#drivereditor [name=membership]').val("");
		$('#drivereditor [name=address]').val("");
		$('#drivereditor [name=city]').val("");
		$('#drivereditor [name=state]').val("");
		$('#drivereditor [name=zip]').val("");
		$('#drivereditor [name=homephone]').val("");
		$('#drivereditor [name=brag]').val("");
		$('#drivereditor [name=sponsor]').val("");
		$('#drivereditor [name=clubs]').val("");
	}

	$('#drivereditor [name=driverid]').val(did);
	$('#drivereditor').dialog('open');
}

$(document).ready(function(){
    $("#drivereditor").validate();

	$("#drivereditor").dialog({
		autoOpen: false,
		height: 350,
		width: 500,
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

<% from nwrsc.model import Driver %>
${driverform(Driver(), '')}

</%def>
