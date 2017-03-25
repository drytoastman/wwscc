
function moveresults(from, to)
{
	$(to).html($(from).html());
	$(to).attr('modified', $(from).attr('modified'));
}

function processNext(json)
{
	if (json.modified > $('#nexte').attr('modified'))
	{
		$('#nexte').html(json.entrantresult);
		$('#nexte').attr('modified', json.modified);
	}
}

function processResults(json)
{
	if (json.modified > $('#firste').attr('modified'))
	{
		moveresults('#firste', '#seconde');
		$('#firste').html(json.entrantresult);
		$('#firste').attr('modified', json.modified);
		$('#entranttabs').tabs('option', 'active', 1);
	}
	else if (json.modified > $('#seconde').attr('modified'))
	{
		$('#seconde').html(json.entrantresult);
		$('#seconde').attr('modified', json.modified);
	}
}

function processTopTimes(json)
{
	var key;
	for (key in json) {
		if (key != "modified") {
			$('#'+key+'cell').html(json[key]);
		}
	}
}

function processLast(json)
{
	if (json.length == 0)
		return;

	if (json[0].modified > lasttime)
	{
		data = json[0];
		lasttime = data.modified;

		delete data['classcode'];
		$.getJSON($.nwr.url_for('results'), data,  processResults);
		$('#runorder').load($.nwr.url_for('runorder')+'?carid='+data.carid);
		$.getJSON($.nwr.url_for('toptimes'), data,  processTopTimes);
		$.getJSON($.nwr.url_for('nexttofinish'), data,  processNext);
	}
}

function updateCheck()
{
	$.ajax({
			dataType: "json",
			url: $.nwr.url_for('last'),
			data: { modified: lasttime },
			success: function(data) { processLast(data); updateCheck(); },
			error: function(xhr) { if (xhr.status != 403) { setTimeout('updateCheck()', 3000); } }
			});
}


function timerUpdater(timer)
{
	$.ajax({
			dataType: "json",
			url: "/timer/"+timer,
			success: function(data) { $('#timeroutput').text(data.timer); timerUpdater(data.timer) },
			error: function(xhr) { if (xhr.status != 403) { setTimeout('timerUpdater("0.000")', 3000); } }
			});
}

$(document).ready(function(){
	$('#toptimetabs').tabs();
	$('#entranttabs').tabs();
	lasttime = 0;
	updateCheck();
	setTimeout('timerUpdater("0.000")', 1000);
});


