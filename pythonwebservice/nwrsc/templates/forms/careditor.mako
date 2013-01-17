
function classchange()
{
	if ($("#careditor [name=classcode] option:selected").attr('indexed'))
	{
		$('.indexcodecontainer').toggle(true);
	}
	else
	{
		$('#careditor [name=indexcode]').val(0);
		$('.indexcodecontainer').toggle(false);
	}

	
	if ($("#careditor [name=classcode] option:selected").attr('usecarflag'))
	{
		$('.tireindexcontainer').toggle(true)
	}
	else
	{
		$('.tireindexcontainer').toggle(false)
	}

	setnum("");
}

function editcar(did, cid)
{
	$('#careditor [name=driverid]').val(did);
	$('#careditor [name=carid]').val(cid);

	if (cid in cars) 
	{
		$('#careditor [name=year]').val(cars[cid].year);
		$('#careditor [name=make]').val(cars[cid].make);
		$('#careditor [name=model]').val(cars[cid].model);
		$('#careditor [name=color]').val(cars[cid].color);
		$('#careditor [name=classcode]').val(cars[cid].classcode);
		$('#careditor [name=indexcode]').val(cars[cid].indexcode);
		$('#careditor [name=tireindexed]').prop('checked', cars[cid].tireindexed);
		classchange();
		setnum(cars[cid].number);
	}
	else
	{
		$('#careditor [name=year]').val("");
		$('#careditor [name=make]').val("");
		$('#careditor [name=model]').val("");
		$('#careditor [name=color]').val("");
		$('#careditor [name=classcode]').val("");
		$('#careditor [name=indexcode]').val("");
		$('#careditor [name=tireindexed]').prop('checked', false);
		setnum("")
	}

	$('#careditor').dialog('open');
}


function setnum(v)
{
	input = $('#careditor [name=number]');
	input.val(v);
	if (input.attr('type') == 'hidden') {
		$('#numberdisplay').html(v);
		if (v != "")
			$('#careditor').validate().form();
	}
	
	$('#numberselection').dialog('close');
}


function setupCarDialog()
{
    $('#careditor').validate({
		rules: {
			indexcode: {
				required: function(element) { return ($("#classcode option:selected").attr('indexed') == '1'); },
			},
			number: {
				required: true,
				min: 1,
				max: 1999,
			}
		},

		messages: { 
			indexcode: 'an index is required for indexed classes',
			number: 'a number is required'
		},

		invalidHandler: function(e, validator) {
			var errors = validator.numberOfInvalids();
			if (errors) {
				$("#carerror").html("");
				$("#carerror").show();
			} else {
				$("#carerror").hide();
			}
		},

		errorPlacement: function(error, element) {
			$("#carerror").append(error);
		},
	});


	$('#careditor').dialog({
		autoOpen: false,
		width: "auto",
		modal: true,
		title: 'Car Editor',
		position: [20, 100],
		buttons: {
			'Ok': function() {
				if ($('#careditor').valid()) {
					$(this).dialog('close');
					caredited();
				}
			},
			Cancel: function() { $(this).dialog('close'); }
		},
		close: function() {
		}
	});


	$('#numberselection').dialog({
		autoOpen: false,
		height: 400,
		width: 480,
		position: [20, 100],
		modal: true,
		title: 'Available Numbers',
		close: function() {}
	});


	$('#careditor [name=classcode]').change(function() { 
		classchange(); 
	});

	classchange();


	$('#numberselect').button().click(function() { 
		$('#numberselection').html("loading...");
		$('#numberselection').dialog('open');
		$('#numberselection').load('${h.url_for(action='carnumbers')}', {
					code : $("#careditor #classcode option:selected").val(),
					driverid : $('#careditor [name=driverid]').val()
				});
	});

}

