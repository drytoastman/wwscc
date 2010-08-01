<%inherit file="base.mako" />
<h3>Driver Editor</h3>

<select multiple='multiple' size='25' name='driver' id='driverlist' style='float: left'>
%for d in c.drivers:
<option driverid='${d.id}'>${d.firstname} ${d.lastname}</option>
%endfor
</select>

<div id='sel' style='float: left'>
<table>
</div>

<br style='clear:both'/>


<script>
$('#driverlist').change(function () {
		var ids = Array();
		$("#driverlist option:selected").each(function () { ids.push($(this).attr('driverid')); });
		$.getJSON('${h.url_for(action='getitems')}', { driverids: ids.join(',') }, function(json) { $("#sel").html(json)}); 
 });


</script>
