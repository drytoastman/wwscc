<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>
<html xmlns='http://www.w3.org/1999/xhtml'>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<title>Scorekeeper Announcer Tool</title>
<link href="/css/custom-theme/jquery-ui-1.8.18.custom.css" rel="stylesheet" type="text/css" />
<script type='text/javascript' src='/js/jquery-1.7.1.min.js'></script>
<script type='text/javascript' src='/js/jquery-ui-1.8.18.custom.min.js'></script>

<style type='text/css'>
.layout td { vertical-align: top; }
.layout table { width: 100%; padding: 5px; }

table { border-collapse: collapse; }
table.runorder th, table.res th, table.stats th, table.toptimes th, table.runorder td, table.res td, table.stats td, table.toptimes td
{ 
	border: 1px black solid; padding: 3px; 
}

.ui-tabs-nav { 
	font-size: 0.7em;
}

.ui-tabs .ui-tabs-panel {
	padding-top: 5px;
	padding-left: 2px;
	padding-right: 2px;
}

tr.header th { 
	font-size: 0.8em;
	font-weight: bold;
	background: #79b7e7; 
}

tr.titles th { 
	font-size: 0.7em;
	font-weight: bold;
	background: #c5dbec; 
}

tr.highlight td {
	font-weight: bold;
	background: #DDD; 
}

table.runorder td, table.res td, table.toptimes td {
	font-size: 0.7em;
}

table.stats td {
	background: #EEE;
	font-size: 0.6em;
}

table.stats td+td {
	font-size: 12px;
	font-family: monospace;
	background: #FFF;
}

#runorder {
	margin-bottom: 10px;
	padding: 4px;
}

#classtable td, #champtable td {
	white-space:nowrap;
}

</style>

<script type="text/javascript">

function openClass(classcode)
{
	alert('open ' + classcode);
}


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
		$('#entranttabs').tabs('option', 'selected', 1);
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
	for (var ii = 1; ii <= ${c.event.getSegmentCount()}; ii++)
	{
		$('#topseg'+ii+'cell').html(json['topseg'+ii]);
	}
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
			$.getJSON('${h.url_for(action='results')}', json.data[ii],  processResults);
		}
		$('#runorder').load('${h.url_for(action='runorder')}?carid='+json.data[0].carid);
		$.getJSON('${h.url_for(action='toptimes')}', json.data[0],  processTopTimes);
		$.getJSON('${h.url_for(action='nexttofinish')}', json.data[0],  processNext);
	}
}

function updateCheck()
{
	$.getJSON('${h.url_for(action='last')}', { time: lasttime }, processLast);
}

$(document).ready(function(){
	$('#toptimetabs').tabs();
	$('#entranttabs').tabs();
	lasttime = 0;
	updateCheck();
	setInterval('updateCheck()', 2500);
});


</script>
</head>

<body>


<table class='layout' id='mainlayout'><tr><td>

<div id='runorder' class='ui-corner-all ui-widget-content'></div>

<div id="entranttabs">
    <ul>
        <li><a href="#nexte"><span>Next to Finish</span></a></li>
        <li><a href="#firste"><span>Last to Finish</span></a></li>
        <li><a href="#seconde"><span>Second to Last</span></a></li>
    </ul>
    <div id="nexte" updated='0'></div>
    <div id="firste" updated='0'></div>
    <div id="seconde" updated='0'></div>
</div>

</td><td>

<div id="toptimetabs">
    <ul>
        <li><a href="#toprawcell"><span>Raw</span></a></li>
        <li><a href="#topnetcell"><span>Net</span></a></li>
%for ii in range(1, c.event.getSegmentCount()+1):
        <li><a href="#topseg${ii}cell"><span>Seg ${ii}</span></a></li>
%endfor
    </ul>
    <div id="toprawcell"></div>
    <div id="topnetcell"></div>
%for ii in range(1, c.event.getSegmentCount()+1):
    <div id="topseg${ii}cell"></div>
%endfor
</div>

</td></tr></table>


</body>
</html>

