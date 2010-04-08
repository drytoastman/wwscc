<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title><?=$results->event->name?></title>
<link rel="stylesheet" type="text/css" href="/css/proresults.css" />
</head>

<body>

<!-- Series info -->
<div id='seriesimage'><img src="../../images/nwr.gif" alt='NWR-SCCA' /></div>
<div id='seriestitle'>2008 Tight N' Tidy ProSolo Series</div>
<div id='sponsortitle'>Sponsored By:<br />
<img src="" alt=' Tight N&#039; Tidy Racing' /></div>

<!-- Event info -->
<div id='eventtitle'><?=$results->event->datestr()?> - <?=$results->event->name?></div>
<div id='entrantcount'>(<?=$results->event->count?> Entrants)</div>

<hr />
<!-- Results -->

<div id='classlinks'>
<?
	foreach ($results->classes as $clsresult)
	{
		echo "<a href='#{$clsresult->class->code}'>{$clsresult->class->code}</a>\n";
	}
?>
</div>

<? classResults($results); ?>

<br/>
<center>
<? topIndexList($topnet, 'Top Indexed Times', 'net'); ?>
<? topTimesList($topraw, 'Top Times', 'praw'); ?>
</center>

<!--#include virtual="/wresfooter.html" -->
</body>
</html>
