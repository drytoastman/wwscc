<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function results_process($rootpath, $startpath, $dbname, $command)
{
	require_once("results/data.php");

	$eventid = array_shift($command);
	if (empty($eventid))
	{
		event_select($startpath);
		echo "<a href='champ/'>Championship</a>\n";
		return;
	}

	echo "
<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>
<html xmlns='http://www.w3.org/1999/xhtml'>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<title>Scorekeeper Results</title>
<link href='$rootpath/css/".loadSetting('results_css')."' rel='stylesheet' type='text/css' /> 
<link href='$rootpath/css/announcer.css' rel='stylesheet' type='text/css' />\n
</head>
<body>
	";

	if ($eventid == 'champ')
	{
		require_once("results/formats/champ/" . loadSetting("results_cformat"));
		echo "<h2>Championship</h2>\n";	
		champReport(loadChampPoints(), getEvents(), getClassesNumOrder());
		return;
	}

	$event = getEvent($eventid);
	echo "<h2>{$event->name}</h2>\n";	

	global $activeClasses;
	$activeClasses = getActiveClasses($eventid);

	$type = array_shift($command);
	if (empty($type))
	{
		require_once('results/indexpage.php');
		indexPage($event, $activeClasses, ($event->ispro) ? getChallenges($eventid) : null);
		return;
	}

	switch ($type)
	{
		case 'class':
			require_once("results/formats/class/" . loadSetting("results_xformat"));
			$classes = $_GET['list'];
			$results = loadSomeClassResults($eventid, $classes);
			$results->event = $event;
			classResults($results);
			break;

		case 'topindex':
			require_once("results/toptimes.php");
			echo "<center>\n";
			topIndexList(loadTopNetTimes($eventid), 'Top Indexed Times');
			if ($event->courses > 1)
				for ($ii = 1; $ii <= $event->courses; $ii++)
					topIndexList(loadTopCourseNetTimes($eventid, $ii), "Top Course $ii Indexed Times");
			echo "</center>\n";
			break;

		case 'topraw':
			require_once("results/toptimes.php");
			echo "<center>\n";
			topTimesList(loadTopRawTimes($eventid), 'Top Times');
			if ($event->courses > 1)
				for ($ii = 1; $ii <= $event->courses; $ii++)
					topTimesList(loadTopCourseRawTimes($eventid, $ii), "Top Course $ii Times");
			echo "</center>\n";
			break;

		case 'all':
			require_once("results/formats/class/" . loadSetting("results_xformat"));
			$results = loadEventResults($eventid);
			$results->event = $event;
			classResults($results);
			break;

		case 'group':
			require_once("results/formats/class/" . loadSetting("results_xformat"));
			$course = $_GET['course'];
			$groups = $_GET['list'];
			echo "<H3>(Run Groups $groups)</H3>\n";	

			$results = loadRunGroupResults($eventid, $course, $groups);
			$results->event = $event;
			classResults($results, $event);
			break;


		case 'audit':
			require_once("results/formats/audit/" . loadSetting("results_aformat"));
			$course = $_GET['course'];
			$group = $_GET['group'];
			$order = $_GET['order'];
			if (empty($order)) $order = 'firstname';

			if ($event->courses > 1)
				echo "<H3>(Course $course/Run Group $group)</H3>\n";	
			else
				echo "<H3>(Run Group $group)</H3>\n";	

			$results = loadAuditResults($eventid, $course, $group, $order);
			auditreport($results, $event->runs, $order);
			break;

		case 'dialins':
			require_once("results/dialinlist.php");
			dialinList($command, loadDialins($eventid));
			break;

		case 'challenge':
			require_once("results/challengereport.php");
			challengeReport(loadChallenge(array_shift($command)));
			break;
			
		case 'post':
			ob_end_clean(); # Clean the buffer as we will replace the current header information

			require_once("results/formats/class/" . loadSetting("results_ecformat"));
			require_once("results/toptimes.php");

			$results = loadEventResults($eventid);
			$topnet = loadTopNetTimes($eventid);
			$topraw = loadTopRawTimes($eventid);

			$event->count = 0;
			foreach ($results->classes as $cls)
				$event->count += count($cls->entrants);

			$results->event = $event;
			include("formats/event/" . loadSetting("results_etformat"));
			exit;

		default:
			break;
	}

	echo "</body></html>\n";
}

?>

