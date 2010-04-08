<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function recalculate()
{
	ob_end_flush(); // 'live' output of progress

	$events = getEvents();

	global $myDB;
	$myDB->closeAll();  // transactions don't like open prep statements

	$getcarids = getps("select distinct carid from runs where eventid=?");
	$getcodes  = getps("select classcode,indexcode from cars where id=?");
	$getactive = getps("select distinct c.classcode from cars as c, runs as r where r.carid=c.id and r.eventid=?");

	echo "<pre>\n";

	try
	{
		$myDB->dbh->beginTransaction();
		$indexArray = getIndexes();

		foreach ($events as $e)
		{
			print "Event: {$e->id}\n";
			$ids = $getcarids->loadList(array($e->id));
	
			foreach ($ids as $id)
			{
				$codes = $getcodes->LoadOne("CarClass", array($id));
				list($index, $str) = getEffectiveIndex($codes->classcode, $codes->indexcode);
				print "\tId: $id ($index, $str)\n";

				for ($course = 1; $course <= $e->courses; $course++)
					processRuns($e->id, $id, $course, $index);
			}

			$classlist = $getactive->LoadList(array($e->id));
			foreach ($classlist as $c)
			{
				print "\tCls: $c\n";
				processClass($c, $e->id, $indexArray);
			}
		}

		$myDB->closeAll(); 
		$myDB->dbh->commit();
	}
	catch (Exception $e)
	{
		echo "Recalculate error <br/><pre>$e</pre>\n";
		$myDB->closeAll(); 
		$myDB->dbh->rollback();
		throw $e;
	}
}

function netsort($a, $b)
{
	return ($a->net*1000 - $b->net*1000); 
}

function rawsort($a, $b)
{ 
	if ($b->status != "OK")
		return -1;
	if ($a->status != "OK")
		return 1;
	return ($a->raw*1000 - $b->raw*1000);
}

function processRuns($eventid, $carid, $course, $index)
{
	$getruns = getps("select * from runs where eventid=? and carid=? and course=?");
	$updaterun = getps("update runs set net=?,iorder=?,norder=? where eventid=? and carid=? and course=? and run=?");

	$runs = $getruns->loadArray("Run", array($eventid, $carid, $course));
	foreach ($runs as $r)
	{
		if ($r->status == "OK")
			$r->net = ($r->raw * $index) + (2 * $r->cones) + (10 * $r->gates);
		else
			$r->net = 999.999;
	}

	$ii = 1;
	usort($runs, "netsort");
	foreach ($runs as $r)
		$r->norder = $ii++;

	$ii = 1;
	usort($runs, "rawsort");
	foreach ($runs as $r)
		$r->iorder = $ii++;

	foreach ($runs as $r)
		$updaterun->execute(array($r->net, $r->iorder, $r->norder, $eventid, $carid, $course, $r->run));
}

function processClass($classcode, $eventid, $indexes)
{
	static $ppointsArray = array(10, 8, 6, 4, 2);

	$getupdated = getps("select carid,updated from eventresults where classcode=? and eventid=?");
	$delresults = getps("delete from eventresults where classcode=? and eventid=?");
	$getresults = getps("select r.carid as carid, c.indexcode as indexcode, SUM(r.net) as sum, COUNT(r.net) as coursecnt " .
				"from runs as r, cars as c " . 
				"where c.id=r.carid and c.classcode=? and r.eventid=? and r.norder=1 " . 
				"group by r.carid order by coursecnt DESC,sum");
	$insertresults = getps("insert into eventresults VALUES (?,?,?,?,?,?,?,?,?,?)");

	$updateMap = $getupdated->loadPairs('carid', 'updated', array($classcode, $eventid));
	$results = $getresults->loadArray("Result", array($classcode, $eventid));
	$delresults->execute(array($classcode, $eventid));	

	/* Now we will loop from 1st to last, calculating points and inserting new event results */
	$position = 1;
	$first = true;
	$basis = 1;
	$prev = 1;
	$basecnt = 1;

	foreach ($results as $rs)
	{
		$sum = $rs->sum; 
		$cnt = $rs->coursecnt;
		$carid = $rs->carid;

		if ($first)
		{
			$basis = $sum;
			$prev = $sum;
			$basecnt = $cnt;
			$first = false;
		}

		if ($cnt == $basecnt)
		{
			$diff = $sum - $prev;
			if (array_key_exists($rs->indexcode, $indexes))
				$diff /= $indexes[$rs->indexcode];
			$points = $basis/$sum*100;
			if ($position <= count($ppointsArray))
				$ppoints = $ppointsArray[$position - 1];
			else
				$ppoints = 0;
		}
		else
		{ // This person ran less courses than the other people
			$diff = 999.999;
			$points = 0;
			$ppoints = 0;
		}

		$updated = $updateMap[$carid];
		$insertresults->execute(array($eventid, $carid, $classcode, $position, 
								$cnt, $sum, $diff, $points, $ppoints, $updated));
		$position++;
		$prev = $sum;
	}	
}

