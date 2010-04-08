<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function hide($val, $hidenum)
{
	$hide = "";
	for ($ii = 0; $ii < $hidenum; $ii++)
		$hide .= $val[$ii];

	for ($ii = $hidenum; $ii < strlen($val); $ii++)
	{
		if ($val[$ii] == ' ')
			$hide .= '&nbsp;&nbsp;';
		else
			$hide .= "*";
	}

	return $hide;
}

function personDisplay($driver)
{
	echo "
	<h2>My Profile</h2>
	<div class='display'>
	<div>{$driver->fullname()}</div>
	<div>{$driver->email}</div>
	<div>".hide($driver->address, 4)."</div>
	<div>{$driver->city} {$driver->state} {$driver->zip}</div>
	<div>".hide($driver->homephone, 6)."</div>
	<br/>
	<div>Brag: {$driver->brag}</div>
	<div>Sponsor: {$driver->sponsor}</div>
	<div>Clubs: {$driver->clubs}</div>
	<div>Member #: {$driver->membernumber}</div>
	</div>
	";
}


function personNew($prefix)
{
	echo "
	<h2>Create Profile</h2>
	<div class='infobox'>
	<ul>
	<li>First name, last name and email are required
	</ul>
	</div>
	";
	personForm("$prefix/post/new", "$prefix/cars", 'Create');
}


function personEdit($prefix, $driverid)
{
	echo "
	<h3>Update Data</h3>
	<div class='infobox'>
	<ul>
	<li>To change information, enter the new information and click update.</li>
	<li>To delete a field enter a single space as the new data.</li>
	</ul>
	</div>
	";
	personForm("$prefix/post/profile", "$prefix/profile", 'Edit');
}


function personForm($posturl, $returnurl, $button)
{
	echo "
	<div style='margin-left:10px;'>
	<form id='profileform' action='$posturl' method='post'>
	<input type='hidden' name='destination' value='$returnurl' />
	<table class='profile'>
	<tr><th>First Name</th><td><input type='text' name='firstname' size='30' /></td></tr>
	<tr><th>Last Name</th><td><input type='text' name='lastname' size='30' /></td></tr>
	<tr><th>Email</th><td><input type='text' name='email' size='30' /></td></tr>
	<tr><th>Address</th><td><input type='text' name='address' size='30' /></td></tr>
	<tr><th>City</th><td><input type='text' name='city' size='30' /></td></tr>
	<tr><th>State</th><td><input type='text' name='state' size='30' /></td></tr>
	<tr><th>Zip</th><td><input type='text' name='zip' size='30' /></td></tr>
	<tr><th>Home Phone</th><td><input type='text' name='homephone' size='30' /></td></tr>
	<tr><th>Brag</th><td><input type='text' name='brag' size='30' /></td></tr>
	<tr><th>Sponsor</th><td><input type='text' name='sponsor' size='30' /></td></tr>
	<tr><th>Member #</th><td><input type='text' name='membernumber' size='30' /></td></tr>
	</table>
	<br/>
	<input class='update' type='submit' value='$button' />
	</form>
	</div>
	";
}

