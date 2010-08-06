<%inherit file="base.mako" />
<%namespace file="/careditor.mako" import="careditor"/>
<%namespace file="/drivereditor.mako" import="drivereditor"/>

<h3>Driver Editor</h3>

<style>
.ui-dialog { font-size: 0.75em !important; }
table.editor td { border: 1px solid #999; height: 20px; }
table.editor th { border: 1px solid #999; font-size: 10px; text-align: right; padding-right: 4px; width: 20px;}
table.editor { border-collapse: collapse; width: 600px;  }
button.editor { font-size: 11px !important; margin-bottom: 3px; }
button.ceditor { font-size: 9px !important; margin-bottom: 3px; margin-top: 2px; }
div.editor { margin-left: 10px; margin-bottom: 15px; }
</style>

<select multiple='multiple' size='30' name='driver' id='driverlist' style='float: left'>
</select>

<div id='driverinfo' style='float: left'>
</div>

<br style='clear:both'/>

${drivereditor()}
${careditor()}

<script>
var saveids = Array();
var cars = Array();
var drivers = Array();

function buildselect(json)
{
	var select = $('#driverlist');
	var options = select.attr('options');
	$('option', select).remove();

	for (idx in json.data)
	{
		var dr = json.data[idx]
		options[idx] = new Option(dr[1] + " " + dr[2], dr[0]);
	}

	if (saveids.length > 0) {
		select.val(saveids);
		select.change();
	} else {
		$("#driverinfo").html("");
	}

}

function deletedriver(did)
{
	$.post('${h.url_for(action='deletedriver')}', { driverid: did }, function() {
		// Note ids to save and then rebuild driverlist and reselect, slow but always a sure sync with database
		saveids = $('#driverlist').val();
		for (var idx in saveids)
		{
			if (saveids[idx] == did)
			{
				saveids.splice(idx, 1);
				break;
			}
		}
		$.getJSON('${h.url_for(action='getdrivers')}', {}, buildselect);
	});
}

function deletecar(cid)
{
	$.post('${h.url_for(action='deletecar')}', { carid: cid }, function() {
		$("#driverlist").change(); // force reload of driver info
	});
}

function mergedriver(did, allids)
{
	$.post('${h.url_for(action='mergedriver')}', { driverid: did, allids: allids.join(',') }, function() {
		saveids = [""+did];
		$.getJSON('${h.url_for(action='getdrivers')}', {}, buildselect);
	});
}

function driveredited()
{
	$.post('${h.url_for(action='editdriver')}', $("#drivereditor").serialize(), function() {
		$("#driverlist").change(); // force reload of driver info
	});
}

function caredited()
{
	$.post('${h.url_for(action='editcar')}', $("#careditor").serialize(), function() {
		$("#driverlist").change(); // force reload of driver info
	});
}


$('#driverlist').change(function () {
		var ids = Array();
		$("#driverlist option:selected").each(function () { ids.push($(this).attr('value')); });
		$.getJSON('${h.url_for(action='getitems')}', { driverids: ids.join(',') }, function(json) {$("#driverinfo").html(json.data)} );
 });


$(document).ready(function() { 
	$.ajaxSetup({ cache: false });
	$.getJSON('${h.url_for(action='getdrivers')}', {}, buildselect);
});
		
</script>
