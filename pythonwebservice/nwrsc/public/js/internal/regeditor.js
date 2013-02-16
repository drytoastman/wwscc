
(function ($) {

	// attached to buttons and keeps counter of number selected/unselected
	function CounterWatch(callback) {
		this.callback = callback;
		this.count = 0;
		this.change = function(incr) {
			if (incr) { this.count++; }
			else { this.count--; }
			this.callback(this.count);
		}
	}


	var methods = {

		checkbutton: function(watcher) {
			// expects a checkbox input as this
			var me = $(this);
			me.data('watcher', watcher); // reset the watcher each time they call

			// init button with click behaviour only once
			if (!me.data('cbuttoninit'))
			{
				me.data('cbuttoninit', true);
				me.button({ icons: {primary:'ui-icon-radio-on'}}).click(function() {
					var me = $(this);
					var checked = me.is(":checked");
					if (checked) {
						me.button('option', { icons: {primary:'ui-icon-check'} });
					} else {
						me.button('option', { icons: {primary:'ui-icon-radio-on'} });
					}

					var w = me.data('watcher');
					if (w) { w.change(checked); }
					me.blur();
				});
			}

			// uncheck control and make sure button is matched up
			me.attr('checked', false).button('option', { icons: { primary:'ui-icon-radio-on'}, disabled: false }).button('refresh');
			return me;
		},


		registerForEvent: function(car, okcallback) {

			var me = $(this);
			me.find('ul.selectableevents li').each(function() {
				var li = $(this);
				var eventid = li.data('eventid');
				li.toggle($.inArray(eventid, car.canregevents) >= 0);
			});
	
			me.find('ul.selectableevents input').RegEdit('checkbutton');
			me.find('[name=carid]').val(car.id);
		
			me.dialog({
				Xwidth: "auto",
				modal: true,
				title: 'Register Car for Events',
				open: function() { me.find('input').blur(); },
				buttons: {
					'Ok': function() {
						me.dialog('close');
						$.post($.nwr.url_for('registerEventsForCar'), me.serialize(), okcallback); 
					},
					Cancel: function() { me.dialog('close'); }
				},
				close: function() {
				}
			});

			return me;
		},


		registerCars: function(theevent, cars, limit, okcallback) {

			var me = $(this);
			me.find('ul.selectablecars li').each(function() {
				var li = $(this);
				var carid = li.data('carid');
				li.toggle($.inArray(theevent.id, cars[carid].canregevents) >= 0);
			});

			var watcher = new CounterWatch(function(count) {
				var dodisable = (count >= limit);
				me.find('ul.selectablecars input:unchecked').button({ disabled: dodisable });
				me.find('.statuslabel').html( dodisable && "Entry Limit Reached" || ""); 
			});

			me.find('ul.selectablecars input').RegEdit('checkbutton', watcher);
			me.find('[name=eventid]').val(theevent.id);
			me.find('.statuslabel').html('You need to "Create New Car" in the Cars Tab')
			for (carid in cars) {  // len(keys) != 0
				me.find('.statuslabel').html("");
				break;
			}
		
			me.dialog({
				Xwidth: "auto",
				modal: true,
				title: 'Register Cars for ' + theevent.name,
				open: function() { me.find('input').blur(); },
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

			return me;
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


