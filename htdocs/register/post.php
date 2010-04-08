<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function handle_post($prefix, $command)
{
	$posttype = array_shift($command);
	$function = "postprocess_$posttype";

	$nologin = (($posttype == 'login') || ($posttype == 'new'));

	if ($nologin || !empty($_SESSION[dbname()]['driverid']))
		if (is_callable($function))
			$function($prefix, $command);

	if (empty($_POST['destination']))
		echo "How did you get here?";
	else
		header("Location: {$_POST['destination']}"); 

	exit;
}

## No authentication needed for login or new

function postprocess_login($prefix, $command)
{
	# defines firstname, lastname, email, homephone
	extract($_POST, EXTR_PREFIX_SAME, "oops");
	
	if ((empty($firstname)) || (empty($lastname)) || (empty($email)))
	{
		$_SESSION['posterror'] = "You didn't enter all the necessary data\n";
	}
	else
	{
		$stmt = getps("select * from drivers where firstname like ? and lastname like ?");
		$results = $stmt->loadIndexArray("Driver", "id", array($firstname."%", $lastname."%"));

		$size = count($results);
		$match = 0;

		foreach ($results as $driver)
		{
			if (!strcasecmp($driver->email, $email))
			{
				$_SESSION[dbname()]['driverid'] = $driver->id;
				$_SESSION[dbname()]['firstname'] = $driver->firstname;
				$_SESSION[dbname()]['lastname'] = $driver->lastname;
				return;
			}
		}

		$_SESSION['posterror'] = "Couldn't find a match for your information, try again.<br>\n";
		$_SESSION['posterror'] .= "If you have never registered before, you can \n";
		$_SESSION['posterror'] .= "<a href='$prefix/new'>create a new profile</a>\n";
	}
}

## Creating a new user
function postprocess_new($prefix, $command)
{
	$keys = "";
	$values = "";

	$dataok = (!empty($_POST['firstname']) && !empty($_POST['lastname']) && !empty($_POST['email']));
	if (!$dataok)
	{
		$_SESSION['posterror'] = "A new profile requires a first name, last name and email.\n";
		return;
	}

	$getd = getps("select * from drivers where firstname like ? and lastname like ? and email like ?");
	$indb = $getd->loadOne("Driver", array($_POST['firstname'], $_POST['lastname'], $_POST['email']));

	if ($indb != null)
	{
		$_SESSION['posterror'] = "This name and email already exist ({$indb->fullname()} - {$indb->email}). ";
		$_SESSION['posterror'] .= "Trying entering it below instead.\n";
		return;
	}

	list($keys, $vals, $markstr) = extractDriverArgs("insert");

	$newd = getps("insert into drivers (".join(',', $keys).") values ($markstr)");
	$newd->execute($vals);
	$id = lastid();

	if ($id > 0)
	{
		$_SESSION[dbname()]['driverid'] = $id;
		$_SESSION[dbname()]['firstname'] = $_POST['firstname'];
		$_SESSION[dbname()]['lastname'] = $_POST['lastname'];
	}
}


##### The following require that the user is logged in

## Adding, deleting or modifying a car
function postprocess_car($prefix, $command)
{
	switch ($_POST['ctype'])
	{
		case "new":
			list($keys, $vals, $markstr) = extractCarArgs("insert");

			# driver id is in SESSION, but not POST
			$keys[] = 'driverid';
			$vals[] = $_SESSION[dbname()]['driverid'];
			$markstr .= ",?";

			$newd = getps("insert into cars (".join(',', $keys).") values ($markstr)");
			$newd->execute($vals);
			break;


		case "modify":
		case "update":
			list($keys, $vals, $setstr) = extractCarArgs("update");
			$vals[] = $_POST['carid'];
			$mod = getps("update cars set $setstr where id=?");
			$mod->execute($vals);
			break;

		case "delete":
			$delete = getps("delete from cars where id=?");
			$delete->execute(array($_POST['carid']));
			break;
	}
}

## Adding, deleting or modifying an event registration
function postprocess_register($prefix, $command)
{
	$eventid = array_shift($command);
	$type = array_shift($command);
	$regid = array_shift($command);
	$carid = $_POST['carid'];

	$event = getEvent($eventid);
	if (empty($event)) return;

	switch ($type)
	{
		case 'add':
			if ($carid <= 0) break;

			$curCount = loadEventCount($event->id);
			if (empty($event->totlimit) || ($curCount < $event->totlimit))
			{
				$insert = getps("insert or ignore into registered (eventid,carid) values (?,?)");
				$insert->execute(array($eventid, $carid));
			}
			else
			{
				$_SESSION['posterror'] = "Sorry, prereg reached its limit of {$event->totlimit} since your last page load\n";
			}
			break;
	

		case 'chg': 
			if ($carid < 0) 
			{
				$delete = getps("delete from registered where eventid=? and id=?");
				$delete->execute(array($eventid, $regid));
			}
			else if ($carid > 0)
			{
				$update = getps("update registered set carid=? where eventid=? and id=?");
				$update->execute(array($carid, $eventid, $regid));
			}
			# if carid is 0, something is wrong
			break;
	}
}

## Editing of a user's profile
function postprocess_profile($prefix, $command)
{
	$driver = getDriver($_SESSION[dbname()]['driverid']);
	if (empty($driver)) return;

	$baddata = (blankstr($_POST['firstname']) || blankstr($_POST['lastname']) || blankstr($_POST['email']));
	if ($baddata)
	{
		$_SESSION['posterror'] = "You cannot delete the firstname, lastname or email.";
		return;
	}

	list($keys, $vals, $setstr) = extractDriverArgs("update");
		
	$vals[] = $driver->id;

	$stmt = getps("update drivers set $setstr where id=?");
	$stmt->execute($vals);
}

?>
