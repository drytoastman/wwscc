<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



require_once("lib.php");

function loadRegEntries($eventid, $order)
{
	$sql = "";
	switch ($order)
	{
		case 'lastname': $sql = "d.lastname COLLATE NOCASE"; break;
		case 'firstname':  $sql = "d.firstname COLLATE NOCASE"; break;
		case 'classlast': $sql = "c.classcode,d.lastname COLLATE NOCASE"; break;
		case 'classfirst': $sql = "c.classcode,d.firstname COLLATE NOCASE"; break;
		case 'classnumber': $sql = "c.classcode,c.number"; break;
	}

	$reglist = getps("select c.*,d.*,c.id as carid from registered as r, cars as c, drivers as d " .
					"where r.eventid=? and r.carid=c.id and c.driverid=d.id order by $sql");
	return $reglist->loadArray("RegEntry", array($eventid));
}

function loadSingleRegEntry($eventid, $carid)
{
	$reglist = getps("select c.*,d.*,c.id as carid from registered as r, cars as c, drivers as d " .
					"where r.eventid=? and r.carid=c.id and c.driverid=d.id and r.carid=?");
	return $reglist->loadArray("RegEntry", array($eventid, $carid));
}

function loadEventEntries($eventid)
{
	$stmt = getps("select c.*,d.firstname,d.lastname,d.email,d.membernumber " .
				"from registered as r, cars as c, drivers as d, classes as cl " .
				"where r.eventid=? and r.carid=c.id and c.driverid=d.id and c.classcode=cl.code " .
				"order by cl.numorder,d.lastname");
	return $stmt->loadArray("RegEntry", array($eventid));
}

function loadCarNumbers()
{
	$getclist = getps("select code from classes order by numorder");
	$getnum = getps("select distinct c.number, d.firstname, d.lastname from cars as c, drivers as d " .
						"where c.driverid=d.id and c.classcode=? order by c.number");

	$clist = $getclist->loadList();
	$ret = array();
	foreach ($clist as $c)
	{
		$ret[$c] = $getnum->loadArray("RegEntry", array($c));
	}
	return $ret;
}


function getPaymentList($eventid)
{
	$getp = getps("select d.firstname,d.lastname,p.* from drivers as d, payments as p where d.id=p.driverid and p.eventid=? order by d.lastname COLLATE NOCASE, d.firstname COLLATE NOCASE");
	return $getp->loadArray("Payment", array($eventid));
}


function lastnamesort($a, $b) { return strcmp($a->lastname, $b->lastname); }

class PaidList
{
	function __construct()
	{
		$this->hash = array();
		$this->before = array();
		$this->after = array();
	}

	function markBefore($p)
	{
		$p->firstname = ucfirst(trim($p->firstname));
		$p->lastname = ucfirst(trim($p->lastname));

		$key = $p->firstname.$p->lastname;
		if (!array_key_exists($key, $this->hash))
		{
			$this->hash[$key] = 1;
			$this->before[] = $p;
		}
	}

	function markAfter($p)
	{
		$p->firstname = ucfirst(trim($p->firstname));
		$p->lastname = ucfirst(trim($p->lastname));

		$key = $p->firstname.$p->lastname;
		if (!array_key_exists($key, $this->hash))
		{
			$this->hash[$key] = 1;
			$this->after[] = $p;
		}
	}

	function sortLists()
	{
		usort($this->before, "lastnamesort");
		usort($this->after, "lastnamesort");
	}
}


# We do it based on name to avoid any unmerged duplicate drivers and use of the 'prevlist'
function getFeeList($eventid)
{
	$store = new PaidList();

	# Unfortunatly, the sqlite trim function isn't available in the regular php versions yet
	$nsql = "select distinct lower(d.firstname) as firstname, lower(d.lastname) as lastname ".
			"from runs as r, cars as c, drivers as d " .
			"where r.carid=c.id and c.driverid=d.id ";

	$getother = getps("select lower(firstname) as firstname, lower(lastname) as lastname from prevlist");
	$data = $getother->loadArray("Driver");
	foreach ($data as $p)
		$store->markBefore($p);


	$getprev = getps("$nsql and r.eventid in (select id from events where date < (select date from events where id=?))");
	$data = $getprev->loadArray("Driver", array($eventid));
	foreach ($data as $p)
		$store->markBefore($p);


	$getdayof = getps("$nsql and r.eventid=?");
	$data = $getdayof->loadArray("Driver", array($eventid));
	foreach ($data as $p)
		$store->markAfter($p);

	$store->sortLists();
	return $store;
}

?>
