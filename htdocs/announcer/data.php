<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function loadRunOrder($eventid, $carid)
{
	$get = getps("select carid from runorder where eventid=? and rungroup=( " .
				"select rungroup from runorder where eventid=? and carid=? " .
				") order by row");

	$drv = getps("select d.firstname, d.lastname,c.*,re.*,ru.* from drivers as d, cars as c, eventresults as re, runs as ru " .
				"where c.driverid=d.id and c.id=? and re.carid=c.id and re.eventid=? and " .
				"ru.carid=c.id and ru.eventid=re.eventid and ru.norder=1");

	$order = $get->loadList(array($eventid, $eventid, $carid));
	$size = count($order);
	$ret = array();
	for ($ii = 0; $ii < $size; $ii++)
	{
		if ($order[$ii] == $carid)
		{
			$ret[] = $drv->loadOne("Entrant", array($order[($ii+1) % $size], $eventid));
			$ret[] = $drv->loadOne("Entrant", array($order[($ii+2) % $size], $eventid));
			$ret[] = $drv->loadOne("Entrant", array($order[($ii+3) % $size], $eventid));
			break;
		}
	}

	return $ret;
}

function loadDriverResults($eventid, $carid)
{
	$get = getps("select * from runs where carid=? and eventid=? order by run");
	return $get->loadIndexArray("Run", "run", array($carid, $eventid));
}

function loadDriverName($carid)
{
	$get = getps("select d.firstname, d.lastname from drivers as d, cars as c where c.id=? and c.driverid=d.id");
	$driver = $get->loadOne("Driver", array($carid));
	return $driver->fullname();
}

function loadClassResultsByCar($eventid, $carid)
{
	$get = getps("select r.*, c.*, d.firstname, d.lastname " .
				"from eventresults as r, cars as c, drivers as d " .
				"where r.carid=c.id and c.driverid=d.id and r.eventid=? and " .
				"r.classcode=(select classcode from cars where id=?) " .
				"order by r.position");

	$results = $get->loadArray("Entrant", array($eventid, $carid));
	$trophydepth = ceil(count($results->entrants) / 3);

	$ii = 0;
	$lasttime = 0;
	foreach ($results as &$entrant)
	{
		# If this class has event trophies and the entrant was in trophy range, mark it
		if (($results->class->etrophy == 1) && (++$ii <= $trophydepth))
			$entrant->trophy = 'T';

		if ($entrant->updated > $lasttime)
		{
			$lasttime = $entrant->updated;
		}
	}

	return array($results, $lasttime);
}

?>
