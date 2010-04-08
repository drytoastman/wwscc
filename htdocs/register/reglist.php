<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function registrationList($event, $reglist)
{
	$current = '';
	$count = count($reglist);
	$classes = getClasses();

	$html = "<h2 align=center>{$event->name} - $count Entries</h2>\n";
	$html .= "<table class='carlist'>\n";
	foreach ($reglist as $reg)
	{
		if ($reg->classcode != $current)
		{
			$cls = $classes[$reg->classcode];
			$html .= "<tr><th colspan='4'>{$cls->code} - {$cls->descrip}</th></tr>\n";
			$current = $reg->classcode;
			$counter = 0;
		}

		$counter++;
		$html .= "<tr><td class='counter'>$counter</td><td class='number'>{$reg->number}</td>";
		$html .= "<td>{$reg->desc()}</td><td>{$reg->fullname()}</td></tr>\n";
	}
	$html .= "</table>\n";

	return $html;
}

?>
