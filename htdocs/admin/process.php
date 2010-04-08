<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function lockedError()
{
	echo "
	<div class='error' style='margin-top:30px;'>
	The database is currently locked for an event, no changes can be made at this point.
	You can only view entries.
	Please try again in a day or two after the event.</div>
	";
}

function admin_process($startpath, $command)
{
	require_once('lib.php');
	require_once('admin/data.php');
	require_once('admin/auth.php');

	adminNav($startpath);

	/* Initial page, no arguments */
	if (empty($command[0]))
	{
		echo "<h2 style='margin-left:35px; margin-top:60px;'>" . dbname() . " Admin</h2>\n";
		return;
	}

	/* Check if this is posted authentication data */
	else if (($command[0] == 'post') && ($command[1] == 'auth'))
	{
		process_auth();
	}

	else if ($command[0] == 'printhelp')
	{
		ob_end_clean();
		include('admin/printhelp.php');
		exit;
	}

	/* Series? */
	else if (!is_numeric($command[0]))
	{
		if (isLocked())
			lockedError();
		else
			seriesAdmin($startpath, $command);
	}

	/* Default to event */
	else
	{
		eventAdmin($startpath, $command);
	}

	adminNavClose();
}


/**
 * Called to handle any series wide administration
 */
function seriesAdmin($startpath, $command)
{
	if (!check_auth($startpath, 99))
		return;

	$com = array_shift($command);
	switch ($com)
	{
		case 'settings':
			require_once("admin/settingsForm.php");
			settingsForm($startpath);
			break;


		case 'editdriver':
			require_once("admin/editDrivers.php");
			editDrivers($startpath);
			break;

		case 'recalc':
			require_once("admin/recalculateResults.php");
			recalculate();
			break;

		case 'createevent':
			require_once("admin/eventEditor.php");
			eventEditor($startpath, null);
			break;

		case 'post':
			require_once("admin/post.php");
			handleAdminPost($command);
			break;

		case 'duplicates':
			require_once("admin/duplicates.php");
			handleDuplicates($command);
			break;

		case 'createfrom':
			newDBForm($startpath);
			break;

		default:
			echo "Invalid command or event id ($command)\n";
			break;
	}

	return;
}


/**
 * Called to handle any event specific administration
 */
function eventAdmin($startpath, $command)
{
	$eventid = array_shift($command);

	if (!check_auth($startpath, $eventid))
		return;

	$type = array_shift($command);
	$writecommand = (($type == 'post') || ($type == 'edit'));

	if ($writecommand && isLocked())
	{
		lockedError();
		return;
	}

	$event = getEvent($eventid);

	require_once("admin/printlists.php");
	switch($type)
	{
		case 'post':
			require_once("admin/post.php");
			handleEventPost($command, $eventid);
			break;

		case 'print':
			require_once('admin/singlecards.php');
			ob_end_clean();
			if ($_GET['carid'])
				printCards($event, loadSingleRegEntry($eventid, $_GET['carid']));
			else
				printCards($event, loadRegEntries($eventid, $_GET['order']));
			exit;

		case 'blank':
			require_once('admin/singlecards.php');
			ob_end_clean();
			printCards($event, null);
			exit;

		case 'numbers':
			ob_end_clean();
			printNumbers(loadCarNumbers());
			break;

		case 'paid':
			ob_end_clean();
			$fees = getFeeList($eventid);
			printNameList("paidlist", "Fees Paid Before " . $event->name, $fees->before);
			break;

		case 'fees':
			ob_end_clean();
			$fees = getFeeList($eventid);
			printNameList("feelist", "Fees Collected At " . $event->name, $fees->after);
			break;

		case 'paypal':
			ob_end_clean();
			printPayPalList($event, getPaymentList($event->id));
			break;

		case 'edit':
			require_once("admin/eventEditor.php");
			eventEditor($startpath, $event);
			break;

		case 'list':
			require_once("admin/entrylist.php");
			entryList($startpath, $event, loadEventEntries($event->id));
			break;

		case '':
			require_once("admin/eventMenu.php");
			eventMenu($startpath, $eventid);
			break;

		default:
			print 'default, how did this happen?';
			break;
	}
}


function adminNav($prefix)
{
	echo "
	<ul id='qm0' class='qmmc'>
	<li><a class='qmparent' href='javascript:void(0);'>Event Admin</a>
		<ul>
	";

	foreach (getEvents() as $event)
		echo "\t\t<li><a href='$prefix/{$event->id}/'>{$event->name}</a></li>\n";

	echo "
		</ul>
	</li>
	<li><a class='qmparent' href='javascript:void(0);'>Series Admin</a>

		<ul>
		<li><a href='$prefix/createevent'>Create Event</a></li>
		<li><a href='$prefix/settings'>Series Settings</a></li>
		<li><a href='$prefix/editdriver'>Edit Drivers</a></li>
		<li><a href='$prefix/editcar'>Edit Cars</a></li>
		<li><a href='$prefix/duplicates'>Find Duplicates</a></li>
		<li><a href='$prefix/recalc'>Recalculate Results</a></li>
		<li><a href='$prefix/createfrom'>Create New From</a></li>
		</ul>
	</li>
	<li class='qmclear'>&nbsp;</li>
	</ul>
	<div class='body'>
	";
}

function newDBForm($prefix)
{
	echo "
	<h4>Enter the new name:</h4>
	<form action='$prefix/post/newdb' method='POST'>
	<input type='hidden' name='destination' value='$prefix'>
	<input type='text' name='newname' size=20>
	<input type='submit' value='Create'>
	</form>
	";
}

function adminNavClose()
{
	echo "
	</div>
	";
}


?>
