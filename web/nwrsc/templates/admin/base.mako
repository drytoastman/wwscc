<%inherit file="/base.mako" />

<script type="text/javascript"> 
    $(document).ready(function(){ 
        $("ul.sf-menu").supersubs({
			minWidth:    10,
            maxWidth:    100,
            extraWidth:  5 
		}).superfish({
			disableHI: true,
			animation:   {height:'show'}, 
			speed: 'fast',
			autoArrows: false,
			delay: 200
		}).addClass('ui-widget-header ui-corner-all').width('100%'); 
		$("ul.sf-menu a").button();
    }); 
</script>

<style>
.sf-menu { float: left; }
</style>

<ul id='adminmenu' class='sf-menu'>
<li><a href='javascript:void(0);'>Event Admin</a>
	<ul>
	%for event in sorted(c.events, key=lambda obj: obj.date):
		<li><a href='${h.url_for(eventid=event.id, action='')}'>${event.name}</a></li>
	%endfor
	</ul>
</li>
<li><a href='javascript:void(0);'>Series Admin</a>
	<ul>
	<li><a href='${h.url_for(eventid='s', action='create')}'>Create Event</a></li>
	<li><a href='${h.url_for(eventid='s', action='classlist')}'>Series Classes</a></li>
	<li><a href='${h.url_for(eventid='s', action='indexlist')}'>Series Indexes</a></li>
	<li><a href='${h.url_for(eventid='s', action='seriessettings')}'>Series Settings</a></li>
	<li><a href='${h.url_for(eventid='s', action='drivers')}'>Driver/Car Editor</a></li>
	<li><a href='${h.url_for(eventid='s', action='recalc')}' target='_blank'>Recalculate Results</a></li>
	<li><a href='${h.url_for(eventid='s', action='cleanup')}'>Remove Unused Registration</a></li>
	<li><a href='${h.url_for(eventid='s', action='purge')}'>Purge Old Drivers and Cars</a></li>
	<li><a href='${h.url_for(eventid='s', action='copyseries')}'>Create New Series From</a></li>
	<li><a href='${h.url_for(eventid='s', action='allfees')}' target='_blank'>All Event 'Fees'</a></li>
	</ul>
</li>
</ul>
<br clear='both'/>


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
<div class='body ui-widget'>
${next.body()}
</div>
