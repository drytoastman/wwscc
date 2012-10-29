<%inherit file="base.mako" />

<style>
ul.columns { list-style: none; padding-left: 0px; margin-top: 0px; float: left; }
h4 { margin: 0px; }
div.uline { border-bottom: 1px solid black; font-weight: bold; font-size: 1.2em; }
</style>

<script>
function recheckall()
{
	var allevents = true;
	var allclasses = true;

	$("input:checkbox").each(function(index) {
		if ($(this).attr('checked')) { return; }
		else if ($(this).attr('name').substring(0, 6) == 'event-') { allevents = false }
		else if ($(this).attr('name').substring(0, 6) == 'class-') { allclasses = false }
	});

	$("[name=all-events]").attr('checked', allevents)
	$("[name=all-class]").attr('checked', allclasses)
}

function allclasschecked()
{
	var setto = false; 
	if ($("[name=all-class]").attr('checked')) { setto = 'checked'; }
	
	$("input:checkbox").each(function(index) {
		if ($(this).attr('name').substring(0, 6) == 'class-') { 
			$(this).attr('checked', setto);
		}
	});
}

function alleventschecked()
{
	var setto = false; 
	if ($("[name=all-events]").attr('checked')) { setto = 'checked'; }
	
	$("input:checkbox").each(function(index) {
		if ($(this).attr('name').substring(0, 6) == 'event-') { 
			$(this).attr('checked', setto);
		}
	});
}


</script>

<h2>Contact List</h2>

<p style='width: 80%'>
Select the classes and events that you wish to limit the contact list to.  If no boxes are selected for a group, then the filter is not applied. The all checkbox can be used to select or deselect all classes or events.
</p>

<form action='${h.url_for(action='sendcontactlist')}' method='POST'>

<div style='float: left;'>
<h4>Classes</h4>
<div class='uline'><input type='checkbox' name='all-class' onclick='allclasschecked();'/>All</div>
<ul class='columns'>
%for ii, cls in enumerate(c.classlist):
	%if (ii % 20) == 0:
		</ul><ul class='columns'>
	%endif
	<li><input type='checkbox' name='class-${cls.code}' onclick='recheckall();'/>${cls.code}</li>
%endfor
</ul>
</div>


<div style='float: left; padding-left: 30px;'>
<h4>Events</h4>
<div class='uline'><input type='checkbox' name='all-events' onclick='alleventschecked();'/>All</div>
<ul class='columns'>
%for ii, event in enumerate(c.events):
	%if (ii % 20) == 0:
		</ul><ul class='columns'>
	%endif
	<li><input type='checkbox' name='event-${event.id}' onclick='recheckall();'/>${event.name}</li>
%endfor
</ul>
</div>


<br style='clear:both'/>

<input type='Submit' name='Submit' value='Get Contact List'>
</form>
