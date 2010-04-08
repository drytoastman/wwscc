<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function runOrderTable($order)
{
	echo "<table class='runorder'><tbody>\n";
	echo "<tr class='header'><th colspan='6'>Next To Finish</th></tr>\n";
	echo "<tr class='titles'><th>Name</th><th>Car</th><th>Class</th><th>Best</th><th>Pos</th><th>Need</th></tr>\n";
	foreach ($order as $ent)
	{
		printf("<tr><td>%s</td><td>%s</td><td>%s</td><td>%0.3lf (%d,%d)</td><td>%d</td><td>%0.3lf</td></tr>\n", 
				$ent->fullname(), $ent->desc(), $ent->classcode, $ent->raw, $ent->cones, $ent->gates, $ent->position, $ent->diff);
	}
	echo "</table>\n";
}

?>
