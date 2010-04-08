<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function check_auth($startpath, $eventid)
{
	if ($_SERVER["REMOTE_ADDR"] == '127.0.0.1')
		return true;

	$event = getEvent($eventid);	
	if (empty($event) && ($eventid != 99))
	{
		print "No such event ($eventid)\n";
		return false;
	}

	$edetails = $_SESSION["autharray"][$eventid];
	$sdetails = $_SESSION["autharray"][99];

	if (!empty($event) && empty($edetails) && empty($sdetails))
	{
		authForm($startpath, $event->name);
		return false;
	}

	if (empty($event) && empty($sdetails))
	{
		authForm($startpath, "the series");
		return false;
	}

	return true;
}


function process_auth()
{
	$password = $_POST['password'];

	$master = loadSetting('password');
	if (crypt($password, $master) == $master)
	{
		$_SESSION["autharray"][99] = $_SERVER["REMOTE_ADDR"];
	}

	$events = getEvents();
	foreach ($events as $e)
	{
		if (crypt($password, $e->password) == $e->password)
		{
			$_SESSION["autharray"][$e->id] = $_SERVER["REMOTE_ADDR"];
		}
	}

	header("Location: {$_POST['destination']}"); 
}


function authForm($prefix, $name)
{
	echo "
	<form action='$prefix/post/auth' method='POST'>
	<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}'>
	
	<P>
	You are not authenticated for editing $name, please enter the password:
	</P>
	<label for='password'>Password</label><input type='password' name='password'/>
	<input type='Submit' name='Submit' value='Submit'>

	</form>
	";
}

?>
