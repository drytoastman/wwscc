<%inherit file="base.mako" />
<%namespace file="/forms/carform.mako" import="carform"/>
<%namespace file="/forms/driverform.mako" import="driverform"/>

<h2>Driver Editor</h2>

<ul>
<li>Select a single driver from the list to edit their information or merge cars
<li>Select multiple drivers from the list to merge the drivers together
<li>Enter text above the driver list to filter the visible list
</ul>

<div id='selectors' style='float:left'>
<input type='text' id='driverregex' onkeyup='filterlist()'>
<select multiple='multiple' size='25' name='driver' id='driverlist'>
</select>
</div>

<div id='driverinfo' style='float: left'>
</div>

<br style='clear:both'/>

${driverform(allowalias=True)}
${carform(False)}

<script>
$(document).ready(function(){
	$.getJSON($.nwr.url_for('getdrivers'), {}, buildselect);
    $('#driverlist').change(function () {
        var ids = Array();
        $("#driverlist option:selected").each(function () { ids.push($(this).attr('value')); });
        $.getJSON($.nwr.url_for('getitems'), { driverids: ids.join(',') }, function(json) {$("#driverinfo").html(json.data)} );
     });
});
</script>
