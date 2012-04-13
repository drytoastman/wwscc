
function editdriverdirect(firstname, lastname, email)
{
	d = Object()
	d.firstname = firstname
	d.lastname = lastname
	d.email = email
	drivers[-2] = d
	editdriver(-2)
}

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
		$('#drivereditor [name=alias]').val(drivers[did].alias);
		$('#drivereditor [name=alias]').html(drivers[did].alias);
	}
	else
	{
		$('#drivereditor [name=firstname]').val("");
		$('#drivereditor [name=lastname]').val("");
		$('#drivereditor [name=alias]').val("");
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

function setupDriverDialog(title)
{
    $('#drivereditor').validate({
		invalidHandler: function(e, validator) {
			var errors = validator.numberOfInvalids();
			if (errors) {
				$("#drivererror").html("first name, last name and unique/email must be at least 3 characters each");
				$("#drivererror").show();
			} else {
				$("#drivererror").hide();
			}
		},
		errorPlacement: function(error, element) {
		},
		onkeyup: false,
		messages: {}
	});

    $('#drivereditor [name=firstname]').rules("add", {required:true, minlength:3});
    $('#drivereditor [name=lastname]').rules("add", {required:true, minlength:3});
    $('#drivereditor [name=email]').rules("add", {required:true, minlength:3});


	$("#drivereditor").dialog({
		autoOpen: false,
		height: 350,
		width: 620,
		modal: true,
		title: title,
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
			$("#drivererror").hide();
		}
	});
};

