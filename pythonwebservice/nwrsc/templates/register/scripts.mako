function updateEvent(eventid)
{
	$.getJSON('${h.url_for(action='getevent')}', {eventid:eventid}, function(json) {
		$("#event"+eventid).html(json.data);
	});
}

function updateCars()
{
	$.getJSON('${h.url_for(action='getcars')}', function(json) { 
		$("#carswrapper").html(json.data);
	});
}

function driveredited()
{
	$.post('${h.url_for(action='editdriver')}', $("#drivereditor").serialize(), function() {
		$.getJSON('${h.url_for(action='getprofile')}', function(json) {
			$("#profilewrapper").html(json.data)
		});
	});
}

function disableBox(id)
{
	$(id+" select").attr("disabled", "disabled");
	$(id+" button").replaceWith("");
}

function registerCar(s, eventid)
{
	var carid = s.options[s.selectedIndex].value;
	$(s).replaceWith("<div class='notifier'>registering...</div>");
	disableBox("#event"+eventid);
	$.post('${h.url_for(action='registercar')}', {eventid:eventid, carid:carid}, function() {
		updateEvent(eventid);
		updateCars();
	});
}

function reRegisterCar(s, eventid, regid)
{
	var carid = s.options[s.selectedIndex].value;
	$(s).replaceWith("<div class='notifier'>updating registration ...</div>");
	disableBox("#event"+eventid);
	$.post('${h.url_for(action='registercar')}', {regid:regid, carid:carid}, function() {
		updateEvent(eventid);
		updateCars();
	});
}

function unregisterCar(s, eventid, regid)
{
	$(s).prev().replaceWith("<div class='notifier'>unregistering...</div>");
	disableBox("#event"+eventid);
	$.post('${h.url_for(action='registercar')}', {regid:regid, carid:-1}, function() {
		updateEvent(eventid); 
		updateCars();
	});
}

