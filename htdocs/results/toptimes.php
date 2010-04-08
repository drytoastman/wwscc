<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function topTimesList($results, $title)
{
	echo "<table class='toptimes'>\n";
	echo "<tr><th class='classhead' colspan='4'>$title</th></tr>\n";
	echo "<tr><th>#</th><th>Name</th><th>Class</th><th>Time</th></tr>\n";

	$ii = 0;
	foreach ($results as $entrant)
	{
		$ii++;
		echo "<tr>\n";
		echo "<td>$ii</td>\n";
		echo "<td>{$entrant->fullname()}</td>\n";
		echo "<td>{$entrant->classcode}</td>\n";
		printf("<td>%0.3f</td>\n", $entrant->toptime);
		echo "</tr>\n";
	}

	echo "</table>\n";
}


function topIndexList($results, $title)
{
	echo "<table class='toptimes'>\n";
	echo "<tr><th class='classhead' colspan='5'>$title</th></tr>\n";
	echo "<tr><th>#</th><th>Name</th><th colspan='2'>Index</th><th>Time</th></tr>\n";

	$ii = 0;
	foreach ($results as $entrant)
	{
		$ii++;
		echo "<tr>\n";
		echo "<td>$ii</td>\n";
		echo "<td>{$entrant->fullname()}</td>\n";
		list($indexVal, $indexStr) = getEffectiveIndex($entrant->classcode, $entrant->indexcode);
		printf("<td>%0.3f</td>\n", $indexVal);
		echo "<td>$indexStr</td>\n";
		printf("<td>%0.3f</td>\n", $entrant->toptime);
		echo "</tr>\n";
	}

	echo "</table>\n";
}

?>

