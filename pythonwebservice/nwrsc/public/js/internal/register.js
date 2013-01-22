
(function( $ ){

	var methods = {

		loadEvent: function(eventid) {  // need to use .get as load will try and use POST with args
			var me = this;
			$.get(url_for('getevent'), {eventid:eventid}, function(data) { me.html(data); });
		},

		loadCars: function() {
			this.load(url_for('getcars'));
		},

		loadProfile: function() {
			this.load(url_for('getprofile'));
		},

		loadNumbers: function(classcode, driverid, callback) {
			this.load(url_for('carnumbers'), {code:classcode, driverid:driverid}, callback);
		},


		unregButton: function(unregcallback) {
			this.find(".unregbutton").button({icons: { primary:'ui-icon-scissors'}, text: false} ).click(function () {
				$this = $(this);
				var regid = $this.data('regid');
				var eventid = $this.data('eventid');

				$this.parent().replaceWith("<div class='notifier'>unregistering...</div>");
				$.nwr.unregisterCar(regid, function() {
					$('#carswrapper').nwr('loadCars');
					$('#event'+eventid).nwr('loadEvent', eventid);
				});
			});
		}

 	};


	$.fn.nwr = function( method ) {
		if ( methods[method] ) {
			return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else {
			$.error( 'Method ' +  method + ' does not exist on nwr' );
		}    
	};

	
	$.nwr = {
		unregisterCar: function(regid, callback) { $.post(url_for('registercar'), {regid:regid, carid:-1}, callback); }, 
		updateDriver: function(args, callback) { $.post(url_for('editdriver'), args, callback); },
		updateCar: function(args, callback) { $.post(url_for('editcar'), args, callback); }
	};


})( jQuery );


function url_for(action)
{
	return window.location.href.substring(0, window.location.href.lastIndexOf('/')+1) + action
}



// specific to template 
function updateProfile() { $('#profilewrapper').nwr('loadProfile'); }
function updateCars() { $("#carswrapper").nwr('loadCars'); }
function updateEvent(eventid) { $('#event'+eventid).nwr('loadEvent', eventid); }
function updateAll() {
	updateCars();
	$('.eventholder.eventopen').each(function () {
		updateEvent($(this).data('eventid'));
	});
}


function selectcarstab()
{
	$("#tabs").tabs('option', 'active', 1);
}


function deletecar(carid)
{
	if (!confirm("Are you sure you wish to delete this car?"))
		return;
	$.post(url_for(action='deletecar'), {carid:carid}, updateCars); // can only delete something not used in events 
}


function registerCar(s, eventid)
{
	var carid = s.options[s.selectedIndex].value;
	$(s).replaceWith("<div class='notifier'>registering...</div>");
	$.post(url_for('registercar'), {eventid:eventid, carid:carid}, function() {
		updateEvent(eventid);
		updateCars();
	});
}

function reRegisterCar(s, eventid, regid)
{
	var carid = s.options[s.selectedIndex].value;
	$(s).replaceWith("<div class='notifier'>updating registration ...</div>");
	$.post(url_for('registercar'), {regid:regid, carid:carid}, function() {
		updateEvent(eventid);
		updateCars();
	});
}

function copylogin(f, l, e, s)
{
	$("#loginForm [name=firstname]").val(f);
	$("#loginForm [name=lastname]").val(l);
	$("#loginForm [name=email]").val(e);
	$("#loginForm [name=otherseries]").val(s);
	$("#loginForm").submit();
}

function unregButtons(jqe)
{
	jqe.find(".unregbutton").button({icons: { primary:'ui-icon-scissors'}, text: false} ).click(function () {
		$this = $(this);
		var regid = $this.data('regid');
		var eventid = $this.data('eventid');

		$this.parent().replaceWith("<div class='notifier'>unregistering...</div>");
		$.nwr.unregisterCar(regid, function() {
			updateCars();
			updateEvent(eventid);
		});
	});
}

function carTabSetup()
{
	$("#carlist .delete").button({icons: { primary:'ui-icon-trash'}, text: false} ).click(function() {
	    deletecar($(this).data('carid'));
	});
	
	$("#carlist .edit").button({icons: { primary:'ui-icon-pencil'}, text: false} ).click(function() {
	    var driverid = $(this).data('driverid')
		var car = cars[$(this).data('carid')];

		$('#careditor').CarEdit('doDialog', driverid, car, function() {
			$.nwr.updateCar($("#careditor").serialize(), updateCars);  // don't update events, can't edit a car used anyhow
		});
	});
	
	$("#carlist .regbutton").button().click( function() {
		var car = cars[$(this).data('carid')]; // pull from global that we set in template

	    $("#registereventform").registerForEventDialog(car, eventnames, function() {
			updateCars();
	        $('#registereventform input:checked').each(function() {
	            var eventid = $(this).prop('name');
				updateEvent(eventid);
	        });
	    });
	});
	
	unregButtons($('#carlist'));
}

function eventTableSetup(jqe)
{
	unregButtons(jqe)
}


$(document).ready(function() {
	$(".editprofile").button().click( function() {
		var driverid = $(this).data('driverid');
		$('#drivereditor').DriverEdit("doDialog", drivers[driverid], function() {
			$.nwr.updateDriver($("#drivereditor").serialize(), updateProfile);
		});
	});

	unregButtons($('#eventsinner'));

	$( document ).ajaxError(function(event, jqxhr, settings, exception) {
		$( "div.log" ).text("ajax error " + exception);
	});

});
