
function processData(json)
{
	if (json.modified > lasttime)
	{
		lasttime = json.modified;

		$('#seconde').html($('#firste').html());
		$('#firste').html(json.last);
		$('#nexte').html(json.next);
		$('#topnetcell').html(json.topnet);
		$('#toprawcell').html(json.topraw);
		$('#runorder').html(json.order);
		$('#entranttabs').tabs('option', 'active', 1);
	}
}

function updateCheck()
{
	$.ajax({
			dataType: "json",
			url: $.nwr.url_for('next'),
			data: { modified: lasttime },
			success: function(data) { processData(data); updateCheck(); },
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


