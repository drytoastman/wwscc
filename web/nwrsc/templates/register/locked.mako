<%inherit file="base.mako" />

<div class='ui-state-error'>
<span class='ui-state-error-text'>
The database is currently locked for an event or administration, no changes can be made at
this point. Please try again in a day or two after the event.
</span>
</div>

<h3>View Enties For</h3>
<ul class='viewlist'>
%for ev in sorted(c.events, key=lambda obj: obj.date):
<li><a href='${h.url_for(action='view', event=ev.id)}'>${ev.name}</a></li>
%endfor
</ul>
