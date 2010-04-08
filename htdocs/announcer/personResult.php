<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function personResultTable($event, $runs, $drivername)
{
	$html = "<table class='res'><tbody>\n";
	$html .= sprintf("<tr class='header'><th colspan='6'>%s (%s)</th></tr>\n", $drivername, date("h:i:s"));
	$html .= sprintf("<tr class='titles'><th>#</th><th>Raw</th><th>Pen</th><th>Net</th></tr>\n", $results[0]->classcode);
	for ($ii = 1; $ii <= $event->runs; $ii++)
	{
		$run = $runs[$ii];
		$html .= sprintf("<tr class='%s'><td>%d</td><td>%0.3lf</td><td>(%d,%d)</td><td>%0.3lf</td></tr>\n",
			($run->norder == 1) ? 'highlight' : '', $ii, $run->raw, $run->cones, $run->gates, $run->net);
	}
	$html .= "</tbody></table>\n";
	return $html;
}

?>
