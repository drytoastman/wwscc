
function setupRegistrationDialogs()
{
	$('#registereventform').dialog({
		autoOpen: false,
		width: "auto",
		modal: true,
		title: 'Register Car for Event',
		position: [20, 100],
		buttons: {
			'Ok': function() {
				$(this).dialog('close');
				registerCars();
			},
			Cancel: function() { $(this).dialog('close'); }
		},
		close: function() {
		}
	});
}


function registerForEvent(carid, eventids)
{
	var ul = $('#registereventform ul').empty();
	if (eventids.length == 0)
	{
		ul.append("<div>No events to register for</div>");
	}
	else
	{
		for (ii = 0; ii < eventids.length; ii++)
		{
			var cevent = cevents[eventids[ii]];
			var id = "eventid-"+cevent.id;
			ul.append("<li><input type='checkbox' id='"+id+"' name='"+cevent.id+"' /><label for='"+id+"'>"+cevent.name+"</label></li>");

		}
	}

	$('#registereventform input').button({ icons: {primary:'ui-icon-radio-on'}}).click(function() {
		var me = $(this);
		if (me.is( ":checked" )) {
			me.button('option', { icons: {primary:'ui-icon-check'} });
		} else {
			me.button('option', { icons: {primary:'ui-icon-radio-on'} });
		}
	}); 
	$("#registereventform [name=carid]").val(carid);
	$('#registereventform').dialog('open');
}

