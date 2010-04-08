<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function resultInfo($results, $runs, $carid)
{
	$one = $two = $last = $runs[count($runs)];
	for ($ii = 0; $ii < count($runs); $ii++)
	{
		if ($runs[$ii] == null)
			continue;

		if ($runs[$ii]->norder == 1)
			$one = $runs[$ii];
		else if ($runs[$ii]->norder == 2)
			$two = $runs[$ii];
	}

	foreach ($results as $r)
	{
		if ($r->carid == $carid)
		{
			$newpos = $r->position;
			break;
		}
	}

	if ($last->run != $one->run)
	{
		$ndiff = $last->net - $one->net;
		$rdiff = $last->raw - $one->raw;
		$origpos = $newpos;
	}
	else
	{
		$ndiff = $last->net - $two->net;
		$rdiff = $last->raw - $two->raw;
		foreach ($results as $r)
		{
			if ($two->net < $r->sum)
			{
				$origpos = $r->position - 1;
				break;
			}
		}
	}

	if (($rdiff > 0) || ($last->cones == 0))
	{
		$theory = 'N/A';
	}
	else 
	{
		$newnet = $last->net - (2 * $last->cones);
		$tpos = $newpos;
		foreach ($results as $r)
		{
			if ($newnet < $r->sum)
			{
				$tpos = $r->position;
				break;
			}
		}

		if ($tpos == $newpos)
			$theory = 'no change';
		else
			$theory = "$newpos to $tpos";
	}


	$html = "<table class='stats'>\n";
	//$html .= "<tr class='header'><th colspan='2'>Stats</th></tr>\n";
	$html .= sprintf("<tr><td>Raw Change</td><td>%+0.3f</td></tr>\n", $rdiff);
	$html .= sprintf("<tr><td>Net Change</td><td>%+0.3f</td></tr>\n", $ndiff);
	$html .= sprintf("<tr><td>Pos Change</td><td>%s</td></tr>\n", ($newpos != $origpos) ? "$origpos to $newpos" : 'none');
	$html .= "<tr><td>Theoretical</td><td>$theory</td></tr>\n";
	$html .= "</table>\n";
	return $html;
}

?>
