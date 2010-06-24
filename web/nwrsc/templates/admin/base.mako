<%inherit file="/base.mako" />
<ul id='qm0' class='qmmc'>
<li><a class='qmparent' href='javascript:void(0);'>Event Admin</a>
	<ul>
	%for event in sorted(c.events, key=lambda obj: obj.date):
		<li><a href='${h.url_for(eventid=event.id, action='')}'>${event.name}</a></li>
	%endfor
	</ul>
</li>
<li><a class='qmparent' href='javascript:void(0);'>Series Admin</a>
	<ul>
	<li><a href='${h.url_for(eventid='s', action='create')}'>Create Event</a></li>
	<li><a href='${h.url_for(eventid='s', action='classlist')}'>Series Classes</a></li>
	<li><a href='${h.url_for(eventid='s', action='indexlist')}'>Series Indexes</a></li>
	<li><a href='${h.url_for(eventid='s', action='seriessettings')}'>Series Settings</a></li>
	<li><a href='${h.url_for(eventid='s', action='recalc')}'>Recalculate Results</a></li>
	<li><a href='${h.url_for(eventid='s', action='cleanup')}'>Remove Unused Registration</a></li>
	<li><a href='${h.url_for(eventid='s', action='allfees')}'>All Event 'Fees'</a></li>
	<li><a href='${h.url_for(eventid='s', action='purge')}'>Purge Old Drivers and Cars</a></li>
	<li><a href='${h.url_for(eventid='s', action='copyseries')}'>Create New Series From</a></li>
	</ul>
</li>
<li class='qmclear'>&nbsp;</li>
</ul>
%if c.isLocked:
<div style='margin-top:5px; margin-left:30px; color:red; font-weight:bold;'>
<span style='text-decoration: line-through;'>
&nbsp;
&nbsp;
&nbsp;
&nbsp;
&nbsp;
</span>
&nbsp;
Locked
&nbsp;
<span style='text-decoration: line-through;'>
&nbsp;
&nbsp;
&nbsp;
&nbsp;
&nbsp;
</span>
</div>
%endif
<div class='body'>
${next.body()}
</div>
