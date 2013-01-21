
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
		updateDriver: function(args, callback) { $.post(url_for('editdriver'), args, callback); }
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
function updateOpenEvent() {
	$('.eventholder.eventopen').each(function () {
		updateEvent($(this).data('eventid'));
	});
}

function selectcarstab()
{
	$("#tabs").tabs('option', 'active', 1);
}

function caredited()
{
	$.post(url_for(action='editcar'), $("#careditor").serialize(), function() {
		updateCars();
		updateOpenEvent();
	});
}

function deletecar(carid)
{
	if (!confirm("Are you sure you wish to delete this car?"))
		return;
	$.post(url_for(action='deletecar'), {carid:carid}, function() {
		updateCars();
		updateOpenEvent();
	});
}


function registerCars()
{
	$.post(url_for('registercars'), $("#registereventform").serialize(), function() {
		updateCars();
		$('#registereventform input:checked').each(function() {
			updateEvent($(this).prop('name'));
		});
	});
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
	    editcar($(this).data('driverid'), $(this).data('carid')); //${c.driverid}, ${car.id});
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
		$('#drivereditor').editDriverDialog(drivers[driverid], function() {
			$.nwr.updateDriver($("#drivereditor").serialize(), updateProfile);
		});
	});

	unregButtons($('#eventsinner'));

	$( document ).ajaxError(function(event, jqxhr, settings, exception) {
		$( "div.log" ).text("ajax error " + exception);
	});

});
