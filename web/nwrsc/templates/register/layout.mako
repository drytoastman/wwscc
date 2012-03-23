<%inherit file="/base.mako" />
<%namespace file="/forms/careditor.mako" import="careditor"/>
<%namespace file="/forms/drivereditor.mako" import="drivereditor"/>
<%namespace file="events.mako" import="eventdisplay"/>
<%namespace file="cars.mako" import="carlist"/>
<%namespace file="profile.mako" import="profile"/>

<style>

#series {
	text-align: left;
	font-family: "Trebuchet MS", Helvetica, sans-serif;
}

#sponsor img {
	display: block;
	float: left;
	margin-left: 40px;
}

#content {
	min-width: 600px;
	width: auto !important;
	width: 600px;
}


.ui-widget {
	font-size: 0.8em !important;
	font-family: "Palatino Linotype", "Book Antiqua", Palatino, serif;
}

.ui-button {
}

.ui-accordion-content {
}

#profile {
	margin: 1px;
	float: left;
	margin-right: 5px;
	font-family: "Trebuchet MS", Helvetica, sans-serif;
}

#profile span {
	display: block;
	font-size: 0.8em;
}

#editprofile {
	margin-top: 10px;
}

#beforeerror, #aftercars {
	clear: both
}

#errormsg {
	margin-top: 20px;
}

#errormsg span {
	margin-left: 20px;
}

#events {
	float: left;
	margin: 1px;
	width: 350px;
	margin-right: 5px;
}

#cars {
	float: left;
	margin: 1px;
}

.clear {
	clear: both;
}

.cardrop {
	color: #aaa;
	text-align: center;
	border: 1px solid #aaa;
	width: 150px;
}

#carlist { list-style-type: none; margin-left: 0px; margin-top: 30px; padding: 0; }

#carlist li { margin: 6px 0px; padding: 0.4em; font-size: 1.0em; height: 18px; }

#createcar { margin-left: 5px; }

.eselector { display: inline !important; }
.eselector + button { height: 20px; }

.espace { height: 10px; }

</style>

<div id='content'>

<div id='series'>
<h2>${c.settings.seriesname}</h2>
</div>


<div id='profile'>
<div id='profilewrapper'>
${profile()}
</div>
<input id='editprofile' type='button' value='Edit' onclick='editdriver(${c.driver.id})'/>
<input id='logout' type='button' value='Logout' onclick='document.location.href="${h.url_for(action="logout")}"'/>
</div>


<div id='sponsor'>
%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>


<div id='beforeerror'></div>

%if len(c.previouserror) > 0:
<div id='errormsg' class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror|n}</span>
</div>
%endif


<div id='events'>
<h2>Events</h2>
<div id='eventsinner'>

%for ev in sorted(c.events, key=lambda obj: obj.date):
	<h3><a>${ev.date.strftime('%a %b %d')} - ${ev.name}</a></h3>
	<div id='event${ev.id}'>
	${eventdisplay(ev)}
	</div>
%endfor

</div>
</div>


<div id='cars'>
<h2>Cars</h2>
<div id='carswrapper'>
${carlist()}
</div>
<input id='createcar' type='button' name='create' value='Create New Car' onclick='editcar(${c.driver.id}, -1);'/>
</div>


<div id='aftercars'>
</div>

</div>


${drivereditor()}
${careditor()}

<script>

$(document).ready(function() {
	$.ajaxSetup({ cache: false });
	$("#eventsinner").accordion();
	$("input[type='button']").button();
});
		

function driveredited()
{
	$.post('${h.url_for(action='editdriver')}', $("#drivereditor").serialize(), function() {
		$.getJSON('${h.url_for(action='getprofile')}', function(json) { $("#profilewrapper").html(json.data)} );
	});
}

function caredited()
{
	$.post('${h.url_for(action='editcar')}', $("#careditor").serialize(), function() {
		$.getJSON('${h.url_for(action='getcars')}', function(json) { $("#carswrapper").html(json.data)} );
		%for ev in c.events:
		%if not ev.closed:
		$.getJSON('${h.url_for(action='getevent')}', {eventid:${ev.id}}, function(json) { $("#event${ev.id}").html(json.data)} );
		%endif
		%endfor
	});
}

</script>

