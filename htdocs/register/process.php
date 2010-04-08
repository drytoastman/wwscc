<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function register_process($startpath, $command)
{
	require_once('register/data.php');

	$type = array_shift($command);

	## If there was a request to logout, clear the saved driver ID now
	if ($type == 'logout')
	{
		session_destroy();
		header("Location: $startpath/events"); 
		return;
	}


	## Decide how we want to label the info in the top left corner
	if (empty($_SESSION[dbname()]['driverid']))
	{
		$label = "\n";
	}
	else
	{
		$details = $_SESSION[dbname()];
		$label = $details['firstname'] . " " . $details['lastname'] . " (" . $details['driverid'] . ")";
	}

	
	## Write out the navigation and header DIV's
	registernav($startpath, $type, $label, $_SESSION['posterror']);
	$_SESSION['posterror'] = "";


	## Callback from PayPal
	if ($type == 'ipn')
	{
		require_once('register/paypal.php');
		process_ipn();
		return;
	}


	## If this is a post, process the posted data based on the post type and quit
	if ($type == 'post')
	{
		require_once('register/post.php');
		handle_post($startpath, $command);
		registernavclose();
		return;
	}

	## Otherwise, if it is viewentries, anyone can view, not just those logged in, quit after done
	if ($type == 'viewentries')
	{
		require_once('register/reglist.php');
		$eventid = $command[0];
		if (empty($eventid))
			echo event_select("$startpath/viewentries");
		else
			echo registrationList(getEvent($eventid), loadEventEntries($eventid));
		registernavclose();
		return;
	}

	if (isLocked())
	{
		echo "
		<div style='margin-top:30px;margin-right:30px;color:red;font-weight:bold;'>
		The database is currently locked for an event or administration, no changes can be made at
		this point.  You can only view entries.  Please try again in a day or two after the event.
		</div>
		";
		registernavclose();
		return;
	}

	# Creating new profile, no login to check
	if ($type == 'new')
	{
		require_once('register/profile.php');
		personNew($startpath);
		registernavclose();
		return;
	}

	## If its not one of the above and they aren't logged in, show the login screen and quit
	if (empty($_SESSION[dbname()]['driverid']))
	{
		require_once('register/login.php');
		loginForm($startpath);
		registernavclose();
		return;
	}

	## Switch on the remaining types to decide what to display
	if ($type == '') $type = 'events';

	switch ($type)
	{
		case 'cars':
			require_once('register/cars.php');
			carDisplay($startpath, $_SESSION[dbname()]['driverid']);
			break;

		case 'events':
			require_once('register/events.php');
			eventDisplay($startpath, $_SESSION[dbname()]['driverid']);
			break;

		case 'profile':
			require_once('register/profile.php');
			personDisplay(getDriver($_SESSION[dbname()]['driverid']));
			personEdit($startpath, $_SESSION[dbname()]['driverid']);
			break;

		case 'available':
			require_once('register/available.php');
			ob_end_clean(); ## Dump the header data, time to be special
			availableList($command[0]);
			exit;
			
		default:
			echo "type is $type\n";
			break;
	}

	## Close the div from the nav bar, etc
	registernavclose();
}


function registernav($path, $type, $label, $msg = "")
{
	global $rootpath;

	if (empty($_SESSION[dbname()]['driverid']))
		$loginclass = 'notloggedin';

	switch ($type)
	{
		case 'events':		$ce = 'current'; break;
		case 'cars':		$cc = 'current'; break;
		case 'profile':		$cp = 'current'; break;
		case 'viewentries':	$cv = 'current'; break;
	}
	
	$sponsorlink = loadsetting('register_sponsorlink');
	$sponsorimage = loadsetting('register_sponsorimage');
	$seriesimage = loadsetting('register_seriesimage');

	echo "
	<div id='nav'>
	<p><img src='$rootpath/images/$seriesimage' alt='Series Image' /></p>
	<p id='currentuser'>Current: $label</p>
	<p/>
	<a/>
	<a class='tab $loginclass $ce' href='$path/events'>Events</a>
	<a class='tab $loginclass $cc' href='$path/cars'>My Cars</a>
	<a class='tab $loginclass $cp' href='$path/profile'>My Profile</a>
	<a class='tab             $cv' href='$path/viewentries'>View Entries</a>
	<a class='tab $loginclass' href='$path/logout'>Logout</a>
	</div>

	<div id='contentpane'>
	<div id='sponser'>
	<a href='$sponsorlink' target='_blank'><img border='0' src='$rootpath/images/$sponsorimage' alt='Sponsor Image'/></a>
	</div>

	<div id='error'>
	$msg
	</div>

	<div id='content'>
	";
}


function registernavclose()
{
	echo "
	</div>
	</div>
	";
}

?>
