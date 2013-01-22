<%inherit file="base.mako" />
<%namespace file="/forms/carform.mako" import="carform"/>
<%namespace file="/forms/driverform.mako" import="driverform"/>
<%namespace file="/forms/registerforms.mako" import="registerforms"/>
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
	<button class='logout'>Logout</button>


<div id='profile'>
<div id='profilewrapper'>
${profile()}
</div>
</div>


<div id='cars'>
<div id='carswrapper'>
${carlist()}
</div>
</div>


<div id='eventsinner'>
%for ii, ev in enumerate(sorted(c.eventmap.values(), key=lambda obj: obj.date)):
	<%
		if ev.date < today:
			accordindex = ii + 1
	%>
			
	<h3 class='${(not ev.opened or ev.closed) and "eventclosed" or "eventopen"}'>
	<span class='rightarrow' style='display:none'>&#9658;</span>
	<span class='downarrow'>&#9660;</span>
	<a>
	<span class='eventday'>${ev.date.strftime('%a')}</span>
	<span class='eventmonth'>${ev.date.strftime('%b')}</span>
	<span class='eventdate'>${ev.date.strftime('%d')}</span>
	<span class='eventname'>${ev.name}</span>
	</a>
	</h3>

	<div id='event${ev.id}' data-eventid='${ev.id}' class='eventholder ${(not ev.opened or ev.closed) and "eventclosed" or "eventopen"}'>
	${eventdisplay(ev)}
	</div>
%endfor
</div>


</div> <!-- tabs -->

${driverform()}
${carform(True)}
${registerforms()}

