<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function eventMenu($prefix, $eventid)
{
	$event = getEvent($eventid);
	echo "
	<div class='emenu'>

	<h2 style='margin-bottom:5px;'>{$event->name} - {$event->datestr()}</h2>

	<div style='margin-left: 20px;'>

	<h3>General Admin</h3>
	<ol>
	<li><a href='$prefix/$eventid/edit'>Edit Event Details</a></li>
	<li><a href='$prefix/$eventid/list'>Entry Admin</a></li>
	</ol>

	<h3>Before The Event</h3>
	<ol>
	<li>Download Cards as PDF (<a href='$prefix/printhelp' target='_blank'>Printing Help</a>), ordered by:
		<ul>
		<li><a href='$prefix/$eventid/print?order=lastname'>Last Name</a></li>
		<li><a href='$prefix/$eventid/print?order=firstname'>First Name</a></li>
		<li><a href='$prefix/$eventid/print?order=classlast'>Class then Last Name</a></li>
		<li><a href='$prefix/$eventid/print?order=classfirst'>Class then First Name</a></li>
		<li><a href='$prefix/$eventid/print?order=classnumber'>Class then Number</a></li>
		</ul>
	</li>

	<li><a href='$prefix/$eventid/blank'>Blank Timing Cards</a></li>
	<li><a href='$prefix/$eventid/numbers'>Used Car Number List</a></li>
	<li><a href='$prefix/$eventid/paid'>Series Fee Paid List</a></li>
	<li><a href='$prefix/$eventid/paypal'>Paypal Transaction List</a></li>
	</ol>

	<h3>After The Event</h3>
	<ol>
	<li><a href='$prefix/$eventid/fees'>Collected Fee List</a></li>
	</ol>
	</div>

	</div>
	";
}

?>
