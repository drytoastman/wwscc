<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



require_once('lib.php');


function loadAuditResults($eventid, $course, $group, $order)
{
	if (empty($order))
		$order = 'firstname';

	if ($order == 'runorder')
	{
		$gete = getps("select r.*,d.firstname,d.lastname,c.* " .
				"from runorder as r, cars as c, drivers as d  " .
				"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.course=? and r.rungroup=? " .
				"order by r.row");
	}
	else
	{
		$gete = getps("select r.*,d.firstname,d.lastname,c.* " .
				"from runorder as r, cars as c, drivers as d  " .
				"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.course=? and r.rungroup=? " .
				"order by d.$order COLLATE NOCASE");
	}

	$getr = getps("select * from runs where carid=? and eventid=? and course=?");

	$entries = $gete->loadArray("Entrant", array($eventid, $course, $group));
	foreach ($entries as &$entrant)
	{
		$entrant->runs = $getr->loadIndexArray("Run", "run", array($entrant->carid, $eventid, $course));
	}
	return $entries;
}


function loadClassResults($eventid, $classcode)
{
	$get = getps("select r.*, c.*, d.firstname, d.lastname " .
				"from eventresults as r, cars as c, drivers as d " .
				"where r.carid=c.id and c.driverid=d.id and r.classcode=? and r.eventid=? " .
				"order by r.position");

	$getc = getps("select * from classes where code=?");
	$getr = getps("select * from runs where carid=? and eventid=?");


	$results = new ClassResults();
	$results->class = $getc->loadOne("CarClass", array($classcode)); 
	$results->entrants = $get->loadArray("Entrant", array($classcode, $eventid));

	$trophydepth = ceil(count($results->entrants) / 3);

	$ii = 0;
	foreach ($results->entrants as &$entrant)
	{
		# If this class has event trophies and the entrant was in trophy range, mark it
		if (($results->class->etrophy == 1) && (++$ii <= $trophydepth))
			$entrant->trophy = 'T';

		$runs = $getr->loadArray("Run", array($entrant->carid, $eventid));
		foreach ($runs as $run)
		{
			$entrant->runs[$run->course][$run->run] = $run;
		}
	}

	return $results;
}


function loadRunGroupResults($eventid, $course, $grouplist)
{
	$cls = getps("select distinct c.classcode from runorder as r, cars as c where r.carid=c.id and ".
				"r.eventid=? and r.course=? and r.rungroup in ($grouplist)");
	$classes = $cls->loadList(array($eventid, $course));

	
	$results = new EventResults();

	foreach ($classes as $c)
		$results->classes[$c] = loadClassResults($eventid, $c);

	return $results;
}


function loadSomeClassResults($eventid, $classlist)
{
	$results = new EventResults();
	$list = explode(",", $classlist);
	foreach ($list as $c)
		$results->classes[$c] = loadClassResults($eventid, $c);

	return $results;
}


function getChallenges($eventid)
{
	$stmt = getps("select * from challenges where eventid=?");
	return $stmt->loadArray("Challenge", array($eventid));
}

function loadEventResults($eventid)
{
	$results = new EventResults();
	foreach (getActiveClasses($eventid) as $c)
		$results->classes[$c] = loadClassResults($eventid, $c);

	return $results;
}


function loadTopCourseRawTimes($eventid, $course=1)
{
	$top = getps("select d.firstname,d.lastname,c.classcode,c.indexcode,(r.raw+2*r.cones+10*r.gates) as toptime " .
			"from runs as r, cars as c, drivers as d " .
			"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.course=? and r.norder=1 " .
			"order by toptime");

	return $top->loadArray("Entrant", array($eventid, $course));
}


function loadTopCourseNetTimes($eventid, $course=1)
{
	$net = getps("select d.firstname,d.lastname,c.classcode,c.indexcode,r.net as toptime " .
			"from runs as r, cars as c, drivers as d " .
			"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.course=? and r.norder=1 " .
			//"and c.indexcode != '' " .
			"order by toptime");

	return $net->loadArray("Entrant", array($eventid, $course));
}


function loadTopRawTimes($eventid)
{
	$top = getps("select d.firstname as firstname, d.lastname as lastname, c.classcode as classcode, " .
			"c.indexcode as indexcode, SUM(r.raw+2*r.cones+10*r.gates) as toptime " .
			"from runs as r, cars as c, drivers as d " .
			"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.norder=1 " .
			"group by c.id order by toptime");

	return $top->loadArray("Entrant", array($eventid));
}


function loadTopNetTimes($eventid)
{
	$net = getps("select d.firstname as firstname, d.lastname as lastname, c.classcode as classcode, " .
			"c.indexcode as indexcode, SUM(r.net) as toptime from runs as r, cars as c, drivers as d " .
			"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.norder=1 " .
			//"and c.indexcode != '' " .
			"group by c.id order by toptime");

	return $net->loadArray("Entrant", array($eventid));
}


function loadChampPoints($class = '%')
{
	$bestof = loadSetting("results_bestof");
	$chmp = getps("select r.points,r.ppoints,r.eventid,r.classcode,d.id,d.firstname,d.lastname ".
				"from eventresults as r, cars as c, drivers as d ".
				"where r.carid=c.id and c.driverid=d.id and r.classcode like ?" .
				"order by r.classcode,d.firstname COLLATE NOCASE,d.lastname COLLATE NOCASE,r.eventid");

	$data = $chmp->loadArray("Entrant", array($class));
	$lastclass = "";

	$ret = array();
	foreach ($data as $e)
	{
		$ret[$e->classcode][$e->id]['name'] = $e->fullname();
		$ret[$e->classcode][$e->id]['points'][$e->eventid] = $e->points;
		$ret[$e->classcode][$e->id]['ppoints'][$e->eventid] = $e->ppoints;
	}

	foreach ($ret as $cls => $people)
	{
		foreach ($people as $did => $results)
		{
			$ret[$cls][$did]['events'] = count($results['points']);
			foreach (array('points', 'ppoints') as $type) # Run same calc for both points and ppoints
			{
				rsort($results[$type]);
				$ret[$cls][$did][$type]['total'] = 0;

				$ii = 0;
				foreach ($results[$type] as $points)
				{
					if ($ii < $bestof)
						$ret[$cls][$did][$type]['total'] += $points;  # Add to total points
					else
						$ret[$cls][$did][$type]['drop'][] = $points;  # Otherwise this is a drop event

					$ii++;
				}
			}
		}
	}

	return $ret;
}


function loadChallenge($challengeid)
{
	$loadc = getps("select * from challenges where id=?");
	$loadr = getps("select * from challengerounds where challengeid=? order by round desc");
	$loadd = getps("select d.firstname, d.lastname, c.classcode, c.indexcode from drivers as d, cars as c where c.driverid=d.id and c.id=?");
	$loadx = getps("select * from challengeruns where id=?");

	$challenge = $loadc->loadOne("Challenge", array($challengeid));
	$challenge->rounds = $loadr->loadArray("ChallengeRound", array($challengeid));
	foreach ($challenge->rounds as &$round)
	{
		$tdriver = $loadd->loadOne("Driver", array($round->car1id));
		$round->car1leftrun = $loadx->loadOne("Run", array($round->car1leftid));
		$round->car1rightrun  = $loadx->loadOne("Run", array($round->car1rightid));
		$round->car1class = $tdriver->classcode;
		$round->car1index = $tdriver->indexcode;
		$round->car1name = "{$tdriver->firstname} {$tdriver->lastname}";

		$bdriver = $loadd->loadOne("Driver", array($round->car2id));
		$round->car2leftrun = $loadx->loadOne("Run", array($round->car2leftid));
		$round->car2rightrun = $loadx->loadOne("Run", array($round->car2rightid));
		$round->car2class = $bdriver->classcode;
		$round->car2index = $bdriver->indexcode;
		$round->car2name = "{$bdriver->firstname} {$bdriver->lastname}";

		if ($round->round == 99)
			$challenge->third = &$round;
		else if ($round->round == 1)
			$challenge->first = &$round;
		else if ($round->round == 0)
			$challenge->winners = &$round;
	}
	return $challenge;
}


function loadDialins($eventid)
{

	$top = getps("select d.firstname as firstname, d.lastname as lastname, c.classcode as classcode, " .
			"c.indexcode as indexcode, c.id as carid, SUM(r.raw) as myraw, f.position as position " .
			"from runs as r, cars as c, drivers as d, eventresults as f " .
			"where r.carid=c.id and c.driverid=d.id and r.eventid=? and r.iorder=1 and f.eventid=? and f.carid=c.id " .
			"group by d.id order by position");

	$net = getps("select sum from eventresults where eventid=? and carid=? and courses=2");


	$list = $top->loadArray("Entrant", array($eventid, $eventid));
	$classref = array();

	foreach ($list as &$e)
	{
		list($e->index, $e->indexStr) = getEffectiveIndex($e->classcode, $e->indexcode);
		$e->net = $net->loadAValue(array($eventid, $e->carid));

		if ($e->position == 1)
		{
			$classref[$e->classcode]['base'] = $e->myraw * $e->index;
			$classref[$e->classcode]['net'] = $e->net;
			$classref[$e->classcode]['link'] = $e;
		}

		# Bonus dial is based on my best raw times
		$e->bonusdial = $e->myraw / 2;

		# Class dial is based on the class leaders best time, need to apply indexing though
		$e->classdial = $classref[$e->classcode]['base'] / $e->index / 2;

		# Diff is the difference between my net and the class leaders net
		$e->diff = $e->net - $classref[$e->classcode]['net'];
	
		# Update the leaders diff	
		if ($e->position == 2)
		{
			$classref[$e->classcode]['link']->diff = -$e->diff;
		}

	}

	return $list;
}

?>
