
function classchange()
{
	if ($("#careditor [name=classcode] option:selected").attr('indexed'))
	{
		$('#careditor [name=indexcode]').removeAttr('disabled');
	}
	else
	{
		$('#careditor [name=indexcode]').val(0);
		$('#careditor [name=indexcode]').attr('disabled', 'disabled');
	}
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
		classchange();
		$('#careditor [name=number]').val(cars[cid].number);
	}
	else
	{
		$('#careditor [name=year]').val("");
		$('#careditor [name=make]').val("");
		$('#careditor [name=model]').val("");
		$('#careditor [name=color]').val("");
		$('#careditor [name=classcode]').val("");
		$('#careditor [name=indexcode]').val("");
		$('#careditor [name=number]').val("");
	}

	$('#careditor').dialog('open');
}


function setnum(v)
{
	$('#careditor [name=number]').val(v);
	$('#numberselection').dialog('close');
}


function setupCarDialog()
{
    $('#careditor').validate({
		rules: {
			indexcode: {
				required: function(element) { return ($("#classcode option:selected").attr('indexed') == '1'); },
			},
			'number': {
				required: true,
				min: 1,
				max: 1999,
			}
		},
		messages: { 
			indexcode: 'an index is required for indexed classes'
		},
	});


	$('#careditor').dialog({
		autoOpen: false,
		height: 300,
		width: 500,
		modal: true,
		title: 'Car Editor',
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
		modal: true,
		title: 'Available Numbers',
		close: function() {}
	});

	$('#careditor [name=classcode]').change(function() { 
		classchange(); 
	});


	$('#numberselect').click(function() { 
		$('#numberselection').html("loading...");
		$('#numberselection').dialog('open');
		$('#numberselection').load('${h.url_for(action='carnumbers')}', {
					code : $("#careditor #classcode option:selected").val(),
					driverid : $('#careditor [name=driverid]').val()
				});
	});

}

