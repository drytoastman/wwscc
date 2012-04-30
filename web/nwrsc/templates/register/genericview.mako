<%def name="viewlist()">
<div class='viewlist'>
<h3>View Enties For</h3>
<ul>
%for ev in sorted(c.events, key=lambda obj: obj.date):
<li><a href='${h.url_for(action='view', event=ev.id)}'>${ev.name}</a></li>
%endfor
</ul>
</div>
</%def>

<%inherit file="/base.mako" />
${viewlist()}

