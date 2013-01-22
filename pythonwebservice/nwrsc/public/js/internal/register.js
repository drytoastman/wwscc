
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



/*** specific to template  *******************************/

function updateProfile() { $('#profilewrapper').nwr('loadProfile'); }
function updateCars() { $("#carswrapper").nwr('loadCars'); }
function updateEvent(eventid) { $('#event'+eventid).nwr('loadEvent', eventid); }
function updateAll() {
	updateCars();
	$('.eventholder.eventopen').each(function () {
		updateEvent($(this).data('eventid'));
	});
}

function editCar(driverid, car) {
	$('#careditor').CarEdit('doDialog', driverid, car, function() {
		$.nwr.updateCar($("#careditor").serialize(), updateCars);  // don't update events, can't edit a car used anyhow
	});
}


/*******************************************************/


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
	$("#carlist .deletecar").button({icons: { primary:'ui-icon-trash'}, text: false} ).click(function() {
		deletecar($(this).data('carid'));
	});
	
	$("#carlist .editcar").button({icons: { primary:'ui-icon-pencil'}, text: false} ).click(function() {
		var driverid = $(this).data('driverid')
		var car = cars[$(this).data('carid')];
		editCar(driverid, car);
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

	$('button.logout').button().click(function() { document.location.href=url_for('logout'); });
 	$('button.createcar').button().click(function() { editCar($(this).data('driverid'), {}); });

	// old base
	$('#serieslinks').css('display', 'none');
	$('#seriestab').click(function() { $('#serieslinks').toggle('blind'); });

	// old login page
    $("#loginForm").validate();
    $("#loginsubmit").button();

	$("button.copylogin").button().click( function() {
		var creds = $(this).data('creds');
		$("#loginForm [name=firstname]").val(creds.firstname);
		$("#loginForm [name=lastname]").val(creds.lastname);
		$("#loginForm [name=email]").val(creds.email);
		$("#loginForm [name=otherseries]").val(creds.series);
		$("#loginForm").submit();
	});

	$("button.createdriver").button().click( function() {
		editdriver(-1)
	});

	// old layout page
	$.ajaxSetup({ cache: false });
	$("#tabs").tabs({active: 1});

	$('#eventsinner > h3').last().addClass('lastevent hidden');
	$('#eventsinner > h3').click(function() {
		if ($(this).next().toggle().css('display') != 'none') {
			$(this).removeClass('hidden');
		} else {
			$(this).addClass('hidden');
		}
		$(this).find(".downarrow").toggle();
		$(this).find(".rightarrow").toggle();
	});
	$('#eventsinner > h3.eventclosed').click();

});
