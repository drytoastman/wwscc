<%inherit file="base.mako" />
<%namespace file="/forms/carform.mako" import="carform"/>
<%namespace file="/forms/driverform.mako" import="driverform"/>
<%namespace file="events.mako" import="eventdisplay"/>
<%namespace file="cars.mako" import="carlist"/>
<%namespace file="profile.mako" import="profile"/>
<%
	import datetime
	accordindex = 0;
	today = datetime.date.today();
 %>


<table id='colcontainer'>
<tr>
<td id='events'>
<h2>Events</h2>
<div id='eventsinner'>

%for ii, ev in enumerate(sorted(c.events, key=lambda obj: obj.date)):
	<%
		if ev.date < today:
			accordindex = ii + 1
	%>
			
	<h3 class='${ev.closed and "eventclosed" or "eventopen"}'>
	<a>
	<span class='eventday'>${ev.date.strftime('%a')}</span>
	<span class='eventmonth'>${ev.date.strftime('%b')}</span>
	<span class='eventdate'>${ev.date.strftime('%d')}</span>
	<span class='eventname'>${ev.name}</span>
	</a>
	</h3>

	<div id='event${ev.id}' class='eventholder'>
	${eventdisplay(ev)}

	</div>
%endfor

</div>
</td>


<td id='rightcol'>

<div id='profile'>
<h2>Profile</h2>
<div id='profilewrapper'>
${profile()}
</div>
<input id='editprofile' type='button' value='Edit' onclick='editdriver(${c.driver.id})'/>
<input id='logout' type='button' value='Logout' onclick='document.location.href="${h.url_for(action="logout")}"'/>
</div>


<div id='cars'>
<h2>Cars</h2>
<div id='carswrapper'>
${carlist()}
</div>
<input id='createcar' type='button' name='create' value='Create New Car' onclick='editcar(${c.driver.id}, -1);'/>
</div>

</td> <!-- rightcol -->
</tr>
</table> <!-- colcontainer -->

${driverform()}
${carform(True)}


<script type='text/javascript'>

$(document).ready(function() {
	$.ajaxSetup({ cache: false });
	$("#eventsinner").accordion({ active: ${accordindex}});
	$("input[type='button']").button();
	setupCarDialog(true);
	setupDriverDialog("Edit Profile");
});
		
function caredited()
{
	$.post('${h.url_for(action='editcar')}', $("#careditor").serialize(), function() {
		updateCars();
		%for ev in c.events:
		%if not ev.closed:
			updateEvent(${ev.id});
		%endif
		%endfor
	});
}

function deletecar(carid)
{
	if (!confirm("Are you sure you wish to delete this car?"))
		return;
	$.post('${h.url_for(action='deletecar')}', {carid:carid}, function() {
		updateCars();
		%for ev in c.events:
		%if not ev.closed:
			updateEvent(${ev.id});
		%endif
		%endfor
	});
}

</script>

