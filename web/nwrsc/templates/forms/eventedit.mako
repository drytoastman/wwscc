<%inherit file="/admin/base.mako" />
<h3>Event Editor</h3>

<form id="eventForm" action="${c.action}" method="post">
<table class="form">

<tr title='the event name, what else'>
<th>Name</th>
<td><input type='text' size='40' name='name' value="${c.event.name}"></td>
</tr>

<tr title='event level password for accessing from admin site'>
<th>Password</th>
<td><input type='text' size='40' name='password' value="${c.event.password}"></td>
</tr>

<tr title='date when the event occurs'>
<th>Event Date</th>
<td><input id='date' type='text' size='40' name='date' value="${c.event.date.strftime("%m/%d/%Y")}"></td>
</tr>

<tr title='when registration should open'>
<th>Registration Opens</th>
<td><input id='opens' type='text' size='40' name='regopened' value="${c.event.regopened.strftime("%m/%d/%Y %H:%M")}"></td>
</tr>

<tr title='when registration should close'>
<th>Registration Closes</th>
<td><input id='closes' type='text' size='40' name='regclosed' value="${c.event.regclosed.strftime("%m/%d/%Y %H:%M")}"></td>
</tr>

<th>Location</th>
<td><input type='text' size='40' name='location' value="${c.event.location}"></td>
</tr>

<tr title='the event sponsor'>
<th>Sponsor</th>
<td><input type='text' size='40' name='sponsor' value="${c.event.sponsor}"></td>
</tr>

<tr title='the hosting club'>
<th>Host</th>
<td><input type='text' size='40' name='host' value="${c.event.host}"></td>
</tr>

<tr title='the course designer'>
<th>Designer</th>
<td><input type='text' size='40' name='designer' value="${c.event.designer}"></td>
</tr>

<tr title='the event chair'>
<th>Chair</th>
<td><input type='text' size='40' name='chair' value="${c.event.chair}"></td>
</tr>

<tr class='advanced' title='Is a practice event, does not count towards points'>
<th>Practice</th>
<td><input type='checkbox' name='practice' ${c.event.practice and "checked='yes'"}></td>
</tr>

<tr class='advanced' title='check if this is a ProSolo style event'>
<th>Is A Pro</th>
<td><input type='checkbox' name='ispro' ${c.event.ispro and "checked='yes'"}></td>
</tr>

<tr class='advanced' title='the number of courses'>
<th>Courses</th>
<td><input type='text' size='4' name='courses' value="${c.event.courses}"></td>
</tr>

<tr class='advanced' title='the number of runs taken'>
<th>Runs</th>
<td><input type='text' size='4' name='runs' value="${c.event.runs}"></td>
</tr>

<tr class='advanced' title='the number of runs counted toward final results'>
<th>Counted Runs</th>
<td><input type='text' size='4' name='countedruns' value="${c.event.countedruns}"></td>
</tr>

<tr class='advanced' title='the penalty for hitting a cone'>
<th>Cone Penalty</th>
<td><input type='text' size='4' name='conepen' value="${c.event.conepen}"></td>
</tr>

<tr class='advanced' title='the penalty for missing a gate'>
<th>Gate Penalty</th>
<td><input type='text' size='4' name='gatepen' value="${c.event.gatepen}"></td>
</tr>

<tr class='advanced' title='a comma separated list of the minimum segment times'>
<th>Segments</th>
<td><input type='text' size='40' name='segments' value="${c.event.segments}"></td>
</tr>

<tr title='the maximum number of cars a single person can preregister'>
<th>Limit Per Person</th>
<td><input type='text' size='40' name='perlimit' value="${c.event.perlimit}"></td>
</tr>

<tr title='the maximum number of preregistered cars for the event'>
<th>Event Limit</th>
<td><input type='text' size='40' name='totlimit' value="${c.event.totlimit}"></td>
</tr>

<tr class='advanced' title='a paypal email address if paypal payments are desired'>
<th>PayPal</th>
<td><input type='text' size='40' name='paypal' value="${c.event.paypal}"></td>
</tr>

<tr class='advanced' title='the cost when using paypal'>
<th>Cost</th>
<td><input type='text' size='40' name='cost' value="${c.event.cost}"></td>
</tr>

<tr class='advanced' title='a mailing address to send payments to, use HTML to format'>
<th>Mailing Address</th>
<td><textarea cols='40' rows='6' name='snail'>${c.event.snail}</textarea></td>
</tr>

<tr class='advanced' title='notes to appears on the registration page, use HTML to format'>
<th>Notes</th>
<td><textarea cols='40' rows='6' name='notes'>${c.event.notes}</textarea></td>
</tr>

<tr>
<td>
<button id='showall' onclick='return showallevent();'>Show All</button>
<button id='hideadv' class='advanced' onclick='return hideadvevent();'>Hide Extra</button>
<input type='submit' value="${c.button}">
</td>
</tr>

</table>
</form>

<script>
function showallevent() 
{ 
	$(".advanced").show(); 
	$("#showall").hide(); 
	return false;
}

function hideadvevent() 
{ 
	$(".advanced").hide(); 
	$("#showall").show(); 
	return false;
}
$("#date").AnyTime_picker( { format: "%m/%d/%Y" } );
$("#opens").AnyTime_picker( { format: "%m/%d/%Y %H:%i" } );
$("#closes").AnyTime_picker( { format: "%m/%d/%Y %H:%i" } );
hideadvevent();
</script>

