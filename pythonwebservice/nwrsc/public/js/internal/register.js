

function updateProfile() { $('#profilewrapper').nwr('loadProfile'); }
function updateCars() { $("#carswrapper").nwr('loadCars'); }
function updateEvent(eventid) {
	$('#event'+eventid).nwr('loadEvent', eventid, function() {
		eventPaneSetup($(this));
	});
 }
function updateAll() {
	updateCars();
	$('.eventholder.eventopen').each(function () {
		updateEvent($(this).data('eventid'));
	});
}

function unregButtons(jqe)
{
	jqe.find(".unregbutton").button({icons: { primary:'ui-icon-scissors'}, text: false} ).click(function () {
		$this = $(this);
		var regid = $this.data('regid');
		var eventid = $this.data('eventid');

		$this.parent().replaceWith("<div class='notifier'>unregistering...</div>");
		$.nwr.unRegisterCar(regid, function() {
			updateCars();
			updateEvent(eventid);
		});
	});
}

function editCar(driverid, car) {
	$('#careditor').CarEdit('doDialog', driverid, car, function() {
		$.nwr.updateCar($("#careditor").serialize(), updateCars);  // don't update events, can't edit a car used anyhow
	});
}

function profileTabSetup()
{
	$(".editprofile").button().click( function() {
		var driverid = $(this).data('driverid');
		$('#drivereditor').DriverEdit("doDialog", drivers[driverid], function() {
			$.nwr.updateDriver($("#drivereditor").serialize(), updateProfile);
		});
	});
}

function carTabSetup()
{
	$("#carlist .deletecar").button({icons: { primary:'ui-icon-trash'}, text: false} ).click(function() {
		var carid = $(this).data('carid');
		if (!confirm("Are you sure you wish to delete this car?"))
			return;
		$.nwr.deleteCar(carid, updateCars); // can only delete something not used in events 
	});
	
	$("#carlist .editcar").button({icons: { primary:'ui-icon-pencil'}, text: false} ).click(function() {
		var driverid = $(this).data('driverid')
		var car = cars[$(this).data('carid')];
		editCar(driverid, car);
	});
	
	$("#carlist .regbutton").button().click( function() {
		var car = cars[$(this).data('carid')]; // pull from global that we set in template

		$("#registereventform").RegEdit('registerForEvent', car, function() {
			updateCars();
			$('#registereventform input:checked').each(function() {
				var eventid = $(this).prop('name');
				updateEvent(eventid);
			});
		});
	});
	
 	$('button.createcar').button().click(function() {
		editCar($(this).data('driverid'), {}); 
	});

	unregButtons($('#carlist'));
}

function eventCollapsable()
{
	$('#eventsinner > h3').last().addClass('lastevent collapsed');
	$('#eventsinner > h3').click(function() {
		if ($(this).next().toggle().css('display') != 'none') {
			$(this).removeClass('collapsed');
		} else {
			$(this).addClass('collapsed');
		}
		$(this).find(".downarrow").toggle();
		$(this).find(".rightarrow").toggle();
	});
	$('#eventsinner > h3.eventclosed').click();
}


function eventPaneSetup(jqe)
{
	jqe.find('.regcarsbutton').button().click( function() {
		var eventid = $(this).data('eventid');
		var eventname = seriesevents[eventid].name;

		$("#registercarform").RegEdit('registerCars', eventid, eventname, cars, 2, "Max cars per entrant met", function() {
			updateCars();
			updateEvent(eventid);
		});
	});

	unregButtons(jqe);
}

function loginPage()
{
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
		$('#drivereditor').DriverEdit("doDialog", {}, function() {
			$("#drivereditor").submit();
		});
	});
}


$(document).ready(function() {

	loginPage();
	eventPaneSetup($('#eventsinner'));
	eventCollapsable();

	$.ajaxSetup({ cache: false });
	$( document ).ajaxError(function(event, jqxhr, settings, exception) {
		alert("Request Error: " + exception);
	});

	$('button.logout').button().click(function() { document.location.href=$.nwr.url_for('logout'); });

	// top tabs
	$('.tablist ul').css('display', 'none');
	$('.tablist .tab').click(function() { $(this).parent().find('ul').toggle('blind'); });

	// layout page
	$("#tabs").tabs({active: 1});
	$(".cartablink").click(function() { $("#tabs").tabs('option', 'active', 1); });

});

