<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function classResultTable($results, $highlightid)
{
	$html = "<table class='res'><tbody>\n";
	$html .= sprintf("<tr class='header'><th colspan='4'>%s</th></tr>\n", $results[0]->classcode);
	$html .= sprintf("<tr class='titles'><th>#</th><th>Name</th><th>Net</th><th>Need</th></tr>\n");
	foreach ($results as $r)
	{
		$html .= sprintf("<tr class='%s'><td>%d</td><td>%s %s</td><td>%0.3f</td><td>%0.3f</td></tr>\n",
				($r->carid == $highlightid) ? 'highlight' : '',
				 $r->position, $r->firstname, $r->lastname, $r->sum, $r->diff);
	}
	$html .= "</tbody></table>\n";
	return $html;
}

?>
