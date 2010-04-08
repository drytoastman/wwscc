<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8" />
<title><?=$event->name?></title>
<link rel="stylesheet" type="text/css" href="wwscc_results.css" />
</head>

<body>

<!-- Series info -->
<div id='seriesimage'><img src="../../images/wwscctire.gif" alt='WWSCC' /></div>
<div id='seriestitle'>2008 Slush Season</div>

<!-- Event info -->
<div id='eventtitle'><?=$event->datestr()?> - <?=$event->name?></div>
<div id='hosttitle'>Hosted By: <span class='host'><?=$event->host?></span></div>
<div id='entrantcount'>(<?=$event->count?> Entrants)</div>
<div class='info'>For Indexed Classes Times in Brackets [] Is Raw Time</div>

<hr />
<!-- Results -->

<div id='classlinks'>
<?
	foreach ($activeClasses as $code)
	{
		echo "<a href='#$code'>$code</a>\n";
	}
?>
</div>

<? classResults($results); ?>

<center>
<? //topIndexList($topnet, 'Top Indexed Times', 'net'); ?>
<? topTimesList($topraw, 'Top Times', 'praw'); ?>
</center>

<!--#include virtual="/wresfooter.html" -->
</body>
</html>
