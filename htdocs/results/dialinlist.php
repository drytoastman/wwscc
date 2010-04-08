<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function diffsort($a, $b)
{
	if ($a->diff > $b->diff) return 1;
	if ($a->diff < $b->diff) return -1;
	return 0;
}

function netsort($a, $b)
{
	if ($a->net > $b->net) return 1;
	if ($a->net < $b->net) return -1;
	return 0;
}

function dialinList($command, $people)
{
	echo "<script type='text/javascript' src='/js/sortabletable.js'></script><h3>Dialins</h3>\n";

	$order = array_shift($command);
	if ($order == 'diff')
	{
		usort($people, "diffsort");
		echo "<h4>Order By Class Diff</h4>\n";
	}
	else if ($order == 'net')
	{
		usort($people, "netsort");
		echo "<h4>Order By Net Time</h4>\n";
	}
	else
	{
		echo "<h4><a href='net'>Order By Net Time</a> or <a href='diff'>Order By Class Diff</a></h3>\n";
		return;
	}

	echo "
		<table class='dialins sortable'>
		<tr>
		<th class='sorttable_nosort'>All</th>
		<th class='sorttable_nosort'>Open</th>
		<th class='sorttable_nosort'>Ladies</th>
		<th>Name</th>
		<th>Class</th>
		<th>Index</th>
		<th>Value</th>
		<th>Net Time</th>
		<th>Class Diff</th>
		<th>Bonus Dial</th>
		<th>Class Dial</th>
		</tr>
	";

	$ii = 0;
	$jj = 0;
	$kk = 0;
	foreach ($people as $entrant)
	{
		$ii++;
		echo "<tr>\n";
		echo "<td>$ii</td>\n";
		if ($entrant->classcode[0] == 'L')
		{
			$kk++;
			echo "<td></td><td>$kk</td>\n";
		}
		else
		{
			$jj++;
			echo "<td>$jj</td><td></td>\n";
		}
		echo "<td>{$entrant->fullname()}</td>\n";
		echo "<td>{$entrant->classcode}</td>\n";
		echo "<td>{$entrant->indexStr}</td>\n";
		printf("<td>%0.3f</td>\n", $entrant->index);
		printf("<td><b>%0.3f</b></td>\n", $entrant->net);
		printf("<td><b>%0.3f</b></td>\n", $entrant->diff);
		printf("<td>%0.3f</td>\n", $entrant->bonusdial);
		printf("<td>%0.3f</td>\n", $entrant->classdial);
		echo "</tr>\n";
	}

	echo "</table>\n";
}


