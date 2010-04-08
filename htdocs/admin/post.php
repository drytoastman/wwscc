<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function handleAdminPost($command)
{
	$posttype = array_shift($command);
	$function = "seriesprocess_$posttype";
	if (is_callable($function))
		$function($command);

	if (empty($_POST['destination']))
		echo "How did you get here?";
	else
		header("Location: {$_POST['destination']}"); 

	exit;
}

function handleEventPost($command, $eventid)
{
	$posttype = array_shift($command);
	$function = "eventprocess_$posttype";
	if (is_callable($function))
		$function($command, $eventid);

	if (empty($_POST['destination']))
		echo "How did you get here?";
	else
		header("Location: {$_POST['destination']}"); 

	exit;
}


function seriesprocess_settings($command)
{
	$stmt = getps("insert or replace into settings values(?,?)");
	foreach ($_POST as $k => $v)
	{
		if (strstr($k, "register_") || (strstr($k, "results_")))
		{
			$stmt->execute(array($k, $v));
		}
	}
}

function seriesprocess_createevent($command)
{
	list($keys, $vals, $markstr) = extractEventArgs("insert");

	$password = $_POST['password'];
	if (!empty($password))
	{
		$keys[] = "password";
		$vals[] = crypt($password);
		$markstr .= ",?";
	}

	$stmt = getps("insert into events (".join(',', $keys).") values ($markstr)");
	$stmt->execute($vals);
}


function seriesprocess_newdb($command)
{
	$newname = $_POST['newname'];
	$oldpath = dbpath(dbname());
	$newpath = dbpath($newname);

	copy($oldpath, $newpath);

	$myDB = new Database($newname);
	$dbh = $myDB->dbh;
	$dbh->exec("begin");
	$dbh->exec("delete from events");
	$dbh->exec("delete from runs");
	$dbh->exec("delete from eventresults");
	$dbh->exec("delete from registered");
	$dbh->exec("delete from runorder");
	$dbh->exec("delete from prevlist");
	$dbh->exec("delete from payments");
	$dbh->exec("end");
	$dbh->exec("vacuum");
	$myDB->dbh = null;
}


function eventprocess_editevent($command, $eventid)
{
	list($keys, $vals, $setstr) = extractEventArgs("update");

	$vals[] = $eventid;

	$stmt = getps("update events set $setstr where id=?");
	$stmt->execute($vals);
}


function eventprocess_delreg($command, $eventid)
{
	$stmt = getps("delete from registered where eventid=? and carid=?");
	$stmt->execute(array($eventid, $_POST['carid']));
}

?>
