<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function announcer($rootpath, $dbname, $eventid)
{
	$url = "$rootpath/announcer/$dbname/$eventid";

echo "
<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>
<html xmlns='http://www.w3.org/1999/xhtml'>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<title>Scorekeeper Announcer Tool</title>
<script type='text/javascript' src='$rootpath/js/jquery-1.2.6.min.js'></script>
<style>

#layout td { vertical-align: top; }
#personrow table, #inforow table, #eventrow table, #champrow table { width: 100%; }
#spacer { height: 20px; }

table { border-collapse: collapse; }
table.runorder th, table.res th, table.runorder td, table.res td, table.stats td { border: 1px black solid; padding: 3px; }

tr.header th { 
	font-size: 0.9em;
	font-weight: bold;
	background: #DDE; 
}

tr.titles th { 
	font-size: 0.7em;
	font-weight: bold;
	background: #EEF; 
}

tr.highlight td {
	font-weight: bold;
	background: #DDD; 
}

table.runorder td, table.res td {
	font-size: 0.75em;
}

table.stats td {
	background: #EEE;
	font-size: 0.8em;
}

table.stats td+td {
	font-size: 12px;
	font-family: monospace;
	background: #FFF;
}

</style>
</head>
<body>

<table id='layout' cellpadding='5'>

<tr id='toprow'>
<td colspan=3 id='runorder'></td>
<td>
Show Standings For:<br/>
<input id='champbutton' type='button' value='Champ' onclick='champclick()'>
<input id='eventbutton' type='button' value='Event' onclick='eventclick()'>
</tr>

<tr id='spacer'>
</tr>

<tr id='personrow'>
<td></td>
<td></td>
<td></td>
<td></td>
</tr>

<tr id='inforow'>
<td></td>
<td></td>
<td></td>
<td></td>
</tr>

<tr id='eventrow'>
<td></td>
<td></td>
<td></td>
<td></td>
</tr>

<tr id='champrow'>
<td></td>
<td></td>
<td></td>
<td></td>
</tr>

</table>


<script>

$(document).ready(function(){
	lasttime = 0
	updateCheck()
	setInterval('updateCheck()', 2500);
	$('#champrow').hide();
	$('#eventbutton').attr('disabled', 'true');
});

function eventclick()
{
	$('#eventrow').show();
	$('#eventbutton').attr('disabled', 'true');
	$('#champrow').hide();
	$('#champbutton').removeAttr('disabled');
}

function champclick()
{
	$('#eventrow').hide();
	$('#eventbutton').removeAttr('disabled');
	$('#champrow').show();
	$('#champbutton').attr('disabled', 'true');
}

function openClass(classcode)
{
	alert('open ' + classcode);
}

function processResults(json)
{
	$('#per'+json.updated).html(json.personresult);
	$('#inf'+json.updated).html(json.inforesult);
	$('#cls'+json.updated).html(json.classresult);
	$('#chp'+json.updated).html(json.champresult);
}

function processLast(json)
{
	if (json.length == 0)
		return;

	if (json[0].updated > lasttime)
	{
		lasttime = json[0].updated;
		for (ii = json.length - 1; ii >= 0; ii--)
		{
			$('#personrow > td:last-child').remove()
			$('#inforow > td:last-child').remove()
			$('#eventrow > td:last-child').remove()
			$('#champrow > td:last-child').remove()

			var cell = document.createElement('td');
			cell.setAttribute('id', 'per'+json[ii].updated); 
			$('#personrow').prepend(cell);

			var cell = document.createElement('td');
			cell.setAttribute('id', 'inf'+json[ii].updated); 
			$('#inforow').prepend(cell);

			var cell = document.createElement('td');
			cell.setAttribute('id', 'cls'+json[ii].updated); 
			$('#eventrow').prepend(cell);

			var cell = document.createElement('td');
			cell.setAttribute('id', 'chp'+json[ii].updated); 
			$('#champrow').prepend(cell);

			$.getJSON('$url/results', json[ii],  processResults);
		}

		$('#runorder').load('$url/runorder/'+json[0].carid);
	}
}

function updateCheck()
{
	$.getJSON('$url/last', { time: lasttime }, processLast);
}

</script>
";
}

?>
