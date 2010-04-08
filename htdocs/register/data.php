<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



require_once("lib.php");

function loadEventCount($eventid)
{
	$stmt = getps("select COUNT(id) from registered where eventid=?");
	return $stmt->loadAValue(array($eventid));
}

function loadEventEntries($eventid)
{
	$stmt = getps("select c.*,d.firstname,d.lastname from registered as r, cars as c, drivers as d, classes as cl " .
				"where r.eventid=? and r.carid=c.id and c.driverid=d.id and c.classcode=cl.code " .
				"order by cl.numorder,d.lastname collate nocase");
	return $stmt->loadArray("RegEntry", array($eventid));
}


function loadDriverList($eventid, $driverid)
{
	$stmt = getps("select r.* from registered as r, cars as c where ".
				"r.eventid=? and r.carid=c.id and c.driverid=? order by r.id");
	return $stmt->loadPairs("id", "carid", array($eventid, $driverid));
}

function loadPaymentList($eventid, $driverid)
{
	$stmt = getps("select * from payments where eventid=? and driverid=?");
	return $stmt->loadArray("Payment", array($eventid, $driverid));
}


function loadDriverCars($driverid)
{
	$reg = getps("select distinct carid from registered as r, cars as c where r.carid=c.id and c.driverid=?");
	$cars = getps("select * from cars where driverid=? order by classcode,number"); 

	$reglist = $reg->loadList(array($driverid));
	$carlist = $cars->loadIndexArray("Car", "id", array($driverid));
	foreach ($carlist as &$car)
	{
		$car->inuse = !(array_search($car->id, $reglist) === FALSE);
	}
	return $carlist;
}

?>

