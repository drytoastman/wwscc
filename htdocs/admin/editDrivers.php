<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function editDrivers($prefix)
{
	$stmt = getps("select * from drivers");
	$list = $stmt->loadArray("Driver");

    echo "
    <h3>Edit Drivers</h3>
    <form action='$prefix/post/nothing' method='POST'>
	<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}'>
    <table class='sortable'>
	<thead>
	<tr><th>First</th><th>Last</th></tr>
	</thead>
	";

	foreach ($list as $d)
	{
		echo "<tr><td>$d->firstname</td><td>$d->lastname</td></tr>\n";
	}

	echo "
    </table>
	<br/>
    <input type='submit' value='Update'>
    </form>
    ";
}

?>
