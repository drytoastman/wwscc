
function moveresults(from, to)
{
	$(to).html($(from).html());
	$(to).attr('updated', $(from).attr('updated'));
}

function processNext(json)
{
	if (json.updated > $('#nexte').attr('updated'))
	{
		$('#nexte').html(json.entrantresult);
		$('#nexte').attr('updated', json.updated);
	}
}

function processResults(json)
{
	if (json.updated > $('#firste').attr('updated'))
	{
		moveresults('#firste', '#seconde');
		$('#firste').html(json.entrantresult);
		$('#firste').attr('updated', json.updated);
		$('#entranttabs').tabs('option', 'active', 1);
	}
	else if (json.updated > $('#seconde').attr('updated'))
	{
		$('#seconde').html(json.entrantresult);
		$('#seconde').attr('updated', json.updated);
	}
}

function processTopTimes(json)
{
	var key;
	for (key in json) {
		if (key != "updated") {
			$('#'+key+'cell').html(json[key]);
		}
	}
}

function processLast(json)
{
	if (json.length == 0)
		return;

	if (json[0].updated > lasttime)
	{
		data = json[0];
		delete data['classcode'];
		lasttime = data.updated;
		$.getJSON($.nwr.event_url_for('results'), data,  processResults);
		$('#runorder').load($.nwr.event_url_for('runorder')+'?carid='+data.carid);
		$.getJSON($.nwr.event_url_for('toptimes'), data,  processTopTimes);
		$.getJSON($.nwr.event_url_for('nexttofinish'), data,  processNext);
	}
}

function updateCheck()
{
	$.getJSON($.nwr.event_url_for('last'), { time: lasttime }, processLast);
}

$(document).ready(function(){
	$('#toptimetabs').tabs();
	$('#entranttabs').tabs();
	lasttime = 0;
	updateCheck();
	setInterval('updateCheck()', 2500);
});


