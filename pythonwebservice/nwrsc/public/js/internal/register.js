

function updateProfile() { $('#profilewrapper').nwr('loadProfile'); }
function updateCars() {
	if ($('#registercarform').is(':data(uiDialog)')) {
		$('#registercarform').dialog('destroy').remove(); // make sure we loose that link
	}
	$("#carswrapper").nwr('loadCars');
}
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

		$this.parent().replaceWith("<div class='strong'>unregistering...</div>");
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
		$(this).blur();
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
		$(this).blur();
		editCar(driverid, car);
	});
	
	$("#carlist .regbutton").button().click( function() {
		var car = cars[$(this).data('carid')]; // pull from global that we set in template
		var thisbutton = $(this);
		thisbutton.blur();
		$("#registereventform").RegEdit('registerForEvent', car, 
			function() {
				thisbutton.parents('.carevents').html("<div class='strong'>registering for events ...</div>");
			},
			function() {
				updateCars();
				$('#registereventform input:checked').each(function() {
					updateEvent($(this).prop('name')); // the input name= contains the eventid
				});
			}
		);
	});
	
 	$('button.createcar').button().click(function() {
		$(this).blur();
		editCar($(this).data('driverid'), {}); 
	});

	unregButtons($('#carlist'));
}

function eventCollapsable()
{
	$('#eventsinner > h3').last().addClass('lastevent');
	$('#eventsinner > h3').click(function() {
		var h3 = $(this);
		var ev = $(this).next();
		
		if (ev.is(':visible')) {  // collapse, then add collapsed indicator
			ev.toggle('blind', function() { h3.addClass('collapsed'); });
		} else { // remove collapsed indicator, then show it
			h3.removeClass('collapsed');
			ev.toggle('blind');
		}
		$(this).find(".downarrow").toggle();
		$(this).find(".rightarrow").toggle();
	});
	$('#eventsinner > h3').filter(':not(.eventopen:first)').click(); // close all except first open event
}


function matchHeight(jqe)
{
	var height = 0;
	jqe.children().map(function() { height = Math.max($(this).height(), height); });
	jqe.children().css('min-height', height);
}

function eventPaneSetup(jqe)
{
	jqe.find('.regcarsbutton').button().click( function() {
		var eventid = $(this).data('eventid');
		var theevent = seriesevents[eventid];
		var regcars = 0;
		$.each(cars, function(i, car) { if ($.inArray(theevent.id, car.canregevents) < 0) { regcars++; } } );

		var limit;
		if (theevent.doublespecial) {  // if doublespecial and have a single entry (appear in canreg), they can reg up to perlimit
			limit = theevent.perlimit - regcars;
		} else {  // otherwise, the event limit is also counted
			limit = Math.min( theevent.totlimit - theevent.count, theevent.perlimit - regcars );
		}

		$(this).blur();
		$("#registercarform").RegEdit('registerCars', theevent, cars, limit, 
			function() {
				container = jqe.find('.carcontainer');
				container.find('ul,button').remove();
				container.append("<div class='strong'>registering cars ...</div>");
			},
			function() {
				updateCars();
				updateEvent(eventid);
			}
		);
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

