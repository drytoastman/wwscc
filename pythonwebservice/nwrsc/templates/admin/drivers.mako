<%inherit file="base.mako" />
<%namespace file="/forms/carform.mako" import="carform"/>
<%namespace file="/forms/driverform.mako" import="driverform"/>

<h2>Driver Editor</h2>

<ul>
<li>Select a single driver from the list to edit their information or merge cars
<li>Select multiple drivers from the list to merge the drivers together
<li>Enter text above the driver list to filter the visible list
</ul>

<style>
.ui-dialog { font-size: 0.75em !important; }
table.editor { border-collapse: collapse; }
table.editor td, table.editor th { border: 1px solid #999; height: 20px; font-size: 0.8em; }
table.editor th { text-align: right; padding-right: 5px; width: 100px; }
table.editor th+td { width: 600px; }
button.editor { font-size: 11px !important; margin-bottom: 3px; }
button.ceditor { font-size: 9px !important; margin-bottom: 3px; margin-top: 2px; }
div.editor { margin-left: 10px; margin-bottom: 15px; width: 650px;}
#driverregex { display: block; margin-bottom: 10px; width: 97%; }
#driverlist { width: 100; }
</style>

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
