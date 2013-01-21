
(function ($) {

	$.fn.registerForEventDialog = function(carid, eventids, okcallback)
	{
		$this = $(this);

		var ul = $this.find('ul').empty();
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
	
		$this.find('input').button({ icons: {primary:'ui-icon-radio-on'}}).click(function() {
			var me = $(this);
			if (me.is( ":checked" )) {
				me.button('option', { icons: {primary:'ui-icon-check'} });
			} else {
				me.button('option', { icons: {primary:'ui-icon-radio-on'} });
			}
		}); 

		$this.find('[name=carid]').val(carid);
	
		$this.dialog({
			width: "auto",
			modal: true,
			title: 'Register Car for Event',
			position: [20, 100],
			buttons: {
				'Ok': function() {
					$(this).dialog('close');
					$.post(url_for('registercars'), $(this).serialize(), okcallback); 
				},
				Cancel: function() { $(this).dialog('close'); }
			},
			close: function() {
			}
		});
	}

}) (jQuery);


