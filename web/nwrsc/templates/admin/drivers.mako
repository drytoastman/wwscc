<%inherit file="base.mako" />
<h3>Driver Editor</h3>

<select multiple='multiple' size='30' name='driver' id='driverlist' style='float: left'>
</select>

<div id='sel' style='float: left'>
</div>

<br style='clear:both'/>

<script>

var saveids = Array();

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
		$("#sel").html("");
	}

}

function editdriver(did)
{
	// open editor
}

function editcar(cid)
{
	// open editor
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


$.ajaxSetup({ cache: false });

$.getJSON('${h.url_for(action='getdrivers')}', {}, buildselect);

$('#driverlist').change(function () {
		var ids = Array();
		$("#driverlist option:selected").each(function () { ids.push($(this).attr('value')); });
		$.getJSON('${h.url_for(action='getitems')}', { driverids: ids.join(',') }, function(json) {$("#sel").html(json.data)} );
 });

</script>
