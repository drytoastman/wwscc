<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function pointssort($a, $b)
{
	return ($b['points']['total'] * 1000) - ($a['points']['total'] * 1000);
}

function champReport($people, $classname, $highlight)
{
	usort($people, "pointssort");

	$html = "<table class='res'><tbody>\n";
	$html .= sprintf ("<tr class='header'><th colspan='4'>%s</th></tr>\n", $classname);
	$html .= sprintf ("<tr class='titles'><th>#</th><th>Name</th><th></th><th>Points</th></tr>\n", $classname);
	$ii = 1;
	foreach ($people as $per)
	{
		$html .= sprintf("<tr class='%s'><td>%d</td><td>%s</td><td>%d</td><td>%0.3lf</td></tr>\n", 
			($per['name']==$highlight)?'highlight':'', $ii, $per['name'], $per['events'], $per['points']['total']);
		$ii++;
	}
	$html .= "</tbody></table>\n";
	return $html;
}

?>
