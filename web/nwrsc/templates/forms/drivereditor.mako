
function editdriver(did)
{
	if (did in drivers)
	{
		$('#drivereditor [name=firstname]').val(drivers[did].firstname);
		$('#drivereditor [name=lastname]').val(drivers[did].lastname);
		$('#drivereditor [name=email]').val(drivers[did].email);
		$('#drivereditor [name=address]').val(drivers[did].address);
		$('#drivereditor [name=city]').val(drivers[did].city);
		$('#drivereditor [name=state]').val(drivers[did].state);
		$('#drivereditor [name=zip]').val(drivers[did].zip);
		$('#drivereditor [name=phone]').val(drivers[did].homephone);
		$('#drivereditor [name=brag]').val(drivers[did].brag);
		$('#drivereditor [name=sponsor]').val(drivers[did].sponsor);
%for field in c.fields:
		$('#drivereditor [name=${field.name}]').val(drivers[did].${field.name});
%endfor
	}
	else
	{
		$('#drivereditor [name=firstname]').val("");
		$('#drivereditor [name=lastname]').val("");
		$('#drivereditor [name=email]').val("");
		$('#drivereditor [name=address]').val("");
		$('#drivereditor [name=city]').val("");
		$('#drivereditor [name=state]').val("");
		$('#drivereditor [name=zip]').val("");
		$('#drivereditor [name=phone]').val("");
		$('#drivereditor [name=brag]').val("");
		$('#drivereditor [name=sponsor]').val("");
%for field in c.fields:
		$('#drivereditor [name=${field.name}]').val("");
%endfor
	}

	$('#drivereditor [name=driverid]').val(did);
	$('#drivereditor').dialog('open');
}

function setupDriverDialog()
{
    $("#drivereditor").validate();

	$("#drivereditor").dialog({
		autoOpen: false,
		height: 350,
		width: 600,
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
};

