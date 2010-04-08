<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function announcer_process($rootpath, $startpath, $dbname, $command)
{
	require_once("announcer/data.php");

	$eventid = array_shift($command);
	if (empty($eventid))
		return;

	$event = getEvent($eventid);
	$type = array_shift($command);
	switch ($type)
	{
		case 'runorder':
			require_once("announcer/runOrder.php");
			$carid = array_shift($command);
			runOrderTable(loadRunOrder($eventid, $carid));
			break;

		case 'champ':
			require_once("results/data.php");
			require_once("announcer/champReport.php");
			$class = array_shift($command);
			$carid = array_shift($command);
			if (!empty($carid))
				$drivername = loadDriverName($carid);
			$champ = loadChampPoints($class);
			champReport($champ[$class], $class, $drivername);
			break;

		case 'results':
			require_once("results/data.php");
			require_once("announcer/personResult.php");
			require_once("announcer/resultInfo.php");
			require_once("announcer/classResult.php");
			require_once("announcer/champReport.php");

			$ret = array();
			$ret['updated'] = $_GET['updated'];

			$carid = $_GET['carid'];
			list($results, $lasttime) = loadClassResultsByCar($eventid, $carid);
			$runs = loadDriverResults($eventid, $carid);
			$name = loadDriverName($carid);

			$class = getClass($results[0]->classcode);
			if ($class->ctrophy)
			{
				$champ = loadChampPoints($results[0]->classcode);
				$ret['champresult'] = champReport($champ[$results[0]->classcode], $results[0]->classcode, $name);
			}
			else
			{
				$ret['champresult'] = "";
			}

			$ret['classresult'] = classResultTable($results, $carid);
			$ret['personresult'] = personResultTable($event, $runs, $name);
			$ret['inforesult'] = resultInfo($results, $runs, $carid);

			echo json_encode($ret);
			break;

		case 'last':
			$gu = getps("select updated,carid from eventresults where eventid=? and updated>? order by updated desc limit 4");
			$pairs = $gu->loadArray("Result", array($eventid, $_GET['time']));
			echo json_encode($pairs);
			break;

		default:
			require_once("announcer/announcer.php");
			announcer($rootpath, $dbname, $eventid);
			break;
	}
}

?>

