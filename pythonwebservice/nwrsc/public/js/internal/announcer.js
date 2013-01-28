
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
	$('#toprawcell').html(json.topraw);
	$('#topnetcell').html(json.topnet);
	/*
	for (var ii = 1; ii <= ${c.event.getSegmentCount()}; ii++)
	{
		$('#topseg'+ii+'cell').html(json['topseg'+ii]);
	}
	*/
}

function processLast(json)
{
	if (json.data.length == 0)
		return;

	if (json.data[0].updated > lasttime)
	{
		lasttime = json.data[0].updated;
		for (var ii = json.data.length - 1; ii >= 0; ii--)
		{
			$.getJSON($.nwr.event_url_for('results'), json.data[ii],  processResults);
		}
		$('#runorder').load($.nwr.event_url_for('runorder')+'?carid='+json.data[0].carid);
		$.getJSON($.nwr.event_url_for('toptimes'), json.data[0],  processTopTimes);
		$.getJSON($.nwr.event_url_for('nexttofinish'), json.data[0],  processNext);
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


