
function url_for(action)
{
	return window.location.href.substring(0, window.location.href.lastIndexOf('/')+1) + action
}

function updateEvent(eventid)
{
	$.getJSON(url_for("getevent"), {eventid:eventid}, function(json) {
		$("#event"+eventid).html(json.data);
	});
}

function updateCars()
{
	$.getJSON(url_for('getcars'), function(json) { 
		$("#carswrapper").html(json.data);
	});
}

function driveredited()
{
	$.post(url_for('editdriver'), $("#drivereditor").serialize(), function() {
		$.getJSON(url_for('getprofile'), function(json) {
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
	$.post(url_for('registercar'), {eventid:eventid, carid:carid}, function() {
		updateEvent(eventid);
		updateCars();
	});
}

function reRegisterCar(s, eventid, regid)
{
	var carid = s.options[s.selectedIndex].value;
	$(s).replaceWith("<div class='notifier'>updating registration ...</div>");
	disableBox("#event"+eventid);
	$.post(url_for('registercar'), {regid:regid, carid:carid}, function() {
		updateEvent(eventid);
		updateCars();
	});
}

function unregisterCar(s, eventid, regid)
{
	$(s).prev().replaceWith("<div class='notifier'>unregistering...</div>");
	disableBox("#event"+eventid);
	$.post(url_for('registercar'), {regid:regid, carid:-1}, function() {
		updateEvent(eventid); 
		updateCars();
	});
}

