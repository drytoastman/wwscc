
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
		$.getJSON($.nwr.url_for('results'), data,  processResults);
		$('#runorder').load($.nwr.url_for('runorder')+'?carid='+data.carid);
		$.getJSON($.nwr.url_for('toptimes'), data,  processTopTimes);
		$.getJSON($.nwr.url_for('nexttofinish'), data,  processNext);
	}
}

function updateCheck()
{
	$.getJSON($.nwr.url_for('last'), { time: lasttime }, processLast);
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
	setInterval('updateCheck()', 2500);
	setTimeout('timerUpdater("0.000")', 1000);
});


