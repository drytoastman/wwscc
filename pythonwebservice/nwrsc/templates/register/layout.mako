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


<div id='tabs'>
	<ul>
		<li><a href="#profile"><span>Profile</span></a></li>
		<li><a href="#cars"><span>Cars</span></a></li>
		<li><a href="#eventsinner"><span>Events</span></a></li>
	</ul>
	<input id='logout' type='button' value='Logout' onclick='document.location.href="${h.url_for(action="logout")}"'/>


<div id='profile'>
<div id='profilewrapper'>
${profile()}
</div>
<input id='editprofile' type='button' value='Edit' onclick='editdriver(${c.driver.id})'/>
</div>


<div id='cars'>
<div id='carswrapper'>
${carlist()}
</div>
<input id='createcar' type='button' name='create' value='Create New Car' onclick='editcar(${c.driver.id}, -1);'/>
</div>


<div id='eventsinner'>
%for ii, ev in enumerate(sorted(c.eventmap.values(), key=lambda obj: obj.date)):
	<%
		if ev.date < today:
			accordindex = ii + 1
	%>
			
	<h3 class='${ev.closed and "eventclosed" or "eventopen"}'>
	<span class='rightarrow'>&#9658;</span>
	<span class='downarrow' style='display:none;'>&#9660;</span>
	<a>
	<span class='eventday'>${ev.date.strftime('%a')}</span>
	<span class='eventmonth'>${ev.date.strftime('%b')}</span>
	<span class='eventdate'>${ev.date.strftime('%d')}</span>
	<span class='eventname'>${ev.name}</span>
	</a>
	</h3>

	<div id='event${ev.id}' class='eventholder' style='display:none;'>
	${eventdisplay(ev)}
	</div>
%endfor
</div>


</div> <!-- tabs -->

${driverform()}
${carform(True)}


<script type='text/javascript'>

$(document).ready(function() {
	$.ajaxSetup({ cache: false });
	$("#tabs").tabs({selected: 2});
	$("input[type='button']").button();
	setupCarDialog(true);
	setupDriverDialog("Edit Profile");

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
});
		

function caredited()
{
	$.post('${h.url_for(action='editcar')}', $("#careditor").serialize(), function() {
		updateCars();
		%for ev in c.eventmap.values():
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
		%for ev in c.eventmap.values():
		%if not ev.closed:
			updateEvent(${ev.id});
		%endif
		%endfor
	});
}

</script>

