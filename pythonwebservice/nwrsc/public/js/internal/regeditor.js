
(function ($) {

	var methods = {

		checkbutton: function() {
			// expects a checkbox input as this
			$this = $(this);

			if (!this.data('cbuttoninit'))
			{
				$this.data('cbuttoninit', true);
				$this.button({ icons: {primary:'ui-icon-radio-on'}}).click(function() {
					var me = $(this);
					if (me.is( ":checked" )) {
						me.button('option', { icons: {primary:'ui-icon-check'} });
					} else {
						me.button('option', { icons: {primary:'ui-icon-radio-on'} });
					}
				});
			}

			$this.attr('checked', false).button('option', { icons: { primary:'ui-icon-radio-on'} }).button('refresh');
		},


		registerForEvent: function(car, events, okcallback) {

			$this = $(this);
	
			var ul = $this.find('ul.selectableevents').empty();
	
			if (car.canregevents.length == 0)
			{
				ul.append("<div>No events to register for</div>");
			}
			else
			{
				for (ii = 0; ii < car.canregevents.length; ii++)
				{
					var id = "eventid-"+car.canregevents[ii];
					var inputname = car.canregevents[ii];
					var name = events[car.canregevents[ii]];
					ul.append("<li><input type='checkbox' id='"+id+"' name='"+inputname+"' /><label for='"+id+"'>"+name+"</label></li>");
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
	
			$this.find('[name=carid]').val(car.id);
		
			$this.dialog({
				width: "auto",
				modal: true,
				title: 'Register Car for Event',
				//position: [20, 100],
				buttons: {
					'Ok': function() {
						$(this).dialog('close');
						$.post($.nwr.url_for('registerEventsForCar'), $(this).serialize(), okcallback); 
					},
					Cancel: function() { $(this).dialog('close'); }
				},
				close: function() {
				}
			});
		},


		registerCars: function(eventid, eventname, cars, okcallback) {

			var me = $(this);
			me.find('ul.selectablecars li').each(function() {
				var $li = $(this);
				var carid = $li.data('carid');
				$li.toggle($.inArray(eventid, cars[carid].canregevents) >= 0);
			});

			me.find('ul.selectablecars input').RegEdit('checkbutton');
			me.find('[name=eventid]').val(eventid);
		
			me.dialog({
				width: "auto",
				modal: true,
				title: 'Register Cars for ' + eventname,
				//position: [20, 100],
				buttons: {
					'Ok': function() {
						me.dialog('close');
						$.post($.nwr.url_for('registerCarsForEvent'), me.serialize(), okcallback); 
					},
					Cancel: function() { me.dialog('close'); }
				},
				close: function() {
				}
			});
		}

	};

	$.fn.RegEdit = function( method ) {
		if ( methods[method] ) {
			return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else {
			$.error( 'Method ' +  method + ' does not exist on RegEdit' );
		}    
	};

}) (jQuery);


