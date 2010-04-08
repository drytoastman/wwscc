<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function challengeReport($challenge)
{
	echo "<style>
		tr.winner { background: #EE8; }
		p.winner { text-align:center; font-size: 1.2em; }
		span.dial { font-weight: bold; }
	</style>\n";

	print "<h1>{$challenge->name}</h1>\n";
	foreach ($challenge->rounds as &$round)
	{
		if (($round->round > 98) || ($round->round <= 1))
			continue;
		roundReport($round);
	}
	roundReport($challenge->third);
	roundReport($challenge->first);
}

function winsBy($round)
{
	if (($round->car1leftrun == null) && ($round->car1rightrun == null))
	{
		return array('', '', 'No runs taken');
	}
	else if ($round->car1leftrun->status != "OK")
	{
		$winner = sprintf("%s wins by default\n", $round->car2name);
		return array('', 'winner', $winner);
	}
	else if ($round->car2rightrun->status != "OK")
	{
		$winner = sprintf("%s wins by default\n", $round->car1name);
		return array('winner', '', $winner);
	}
	else if ($round->car1rightrun->status != "OK")
	{
		$winner = sprintf("%s wins by default\n", $round->car2name);
		return array('', 'winner', $winner);
	}
	else if ($round->car2leftrun->status != "OK")
	{
		$winner = sprintf("%s wins by default\n", $round->car1name);
		return array('winner', '', $winner);
	}
	else if ($round->car1result < $round->car2result)
	{
		$winner = sprintf("%s wins by %0.3f\n", $round->car1name, ($round->car2result - $round->car1result));
		return array('winner', '', $winner);
	}
	else if ($round->car2result < $round->car1result)
	{
		$winner = sprintf("%s wins by %0.3f\n", $round->car2name, ($round->car1result - $round->car2result));
		return array('', 'winner', $winner);
	}
	
	return array('', '', '');
}

function roundReport($round)
{
	switch ($round->round)
	{
		case 31: echo "<hr><h2>Bittersweet Thirty-two</h2>\n"; break;
		case 15: echo "<hr><h2>Sweet Sixteen</h2>\n"; break;
		case 7:  echo "<hr><h2>Quarter Finals</h2>\n"; break;
		case 3:  echo "<hr><h2>Semi Finals</h2>\n"; break;
		case 99: echo "<hr><h2>Third Place</h2>\n"; break;
		case 1:  echo "<hr><h2>Final</h2>\n"; break;
	}

	if (($round->car1id <= 0) || ($round->car2id <= 0))
		return;

	$run1l = &$round->car1leftrun;
	$run1r = &$round->car1rightrun;
	$run2l = &$round->car2leftrun;
	$run2r = &$round->car2rightrun;
	list($topclass, $botclass, $winstring) = winsBy($round);

	echo "<table style='width:700px;' class='challengeround' border=1>\n";
	echo "<tr><th>Entrant<th><th>Reaction<th>Sixty<th>Time<th>Diff<th>Total<th>NewDial</tr>\n";
	echo "<tr class='$topclass'>\n";
	driverCell($round->car1name, $round->car1dial, $round->car1class, $round->car1index);
	echo "<td>L</td>\n";
	runRow($round->car1dial, $round->car1leftrun);
	runTotal($round->car1dial, $round->car1leftrun, $round->car1rightrun, $round->car1newdial);
	echo "</tr>\n";
	echo "<tr class='$topclass'>\n";
	echo "<td>R</td>\n";
	runRow($round->car1dial, $round->car1rightrun);
	echo "</tr>\n";
	echo "<tr class='$botclass'>\n";
	driverCell($round->car2name, $round->car2dial, $round->car2class, $round->car2index);
	echo "<td>L</td>\n";
	runRow($round->car2dial, $round->car2leftrun);
	runTotal($round->car2dial, $round->car2leftrun, $round->car2rightrun, $round->car2newdial);
	echo "</tr>\n";
	echo "<tr class='$botclass'>\n";
	echo "<td>R</td>\n";
	runRow($round->car2dial, $round->car2rightrun);
	echo "</tr>\n";
	echo "</table>\n";
	echo "<P class='winner'>$winstring</P>\n";
}

function driverCell($name, $dial, $class, $index)
{
	printf("<td rowspan=2><span class='name'>%s</span><span class='dial'>%0.3f</span><br/>\n", $name, $dial);
	printf("<span class='class'>%s</span><span class='index'>(%s)</span>\n", $class, $index);
}

function runRow($dial, $run)
{
	printf("<td>%0.3f</td><td>%0.3f</td><td>%0.3f (+%d)</td>", $run->reaction, $run->sixty, $run->raw, $run->cones);
	if ($run->status != "OK")
		printf("<td>%s</td>\n", $run->status);
	else
		printf("<td>%+0.3f</td>\n", ($run->net - $dial));
}

function runTotal($dial, $left, $right, $newdial)
{
	echo "<td rowspan=2>\n";
	if (($left->status != "OK") && ($left->status != ""))
		printf("%s</td>\n", $left->status);
	else if (($right->status != "OK") && ($right->status != ""))
		printf("%s</td>\n", $right->status);
	else if (($left->net == 0.0) || ($right->net == 0.0))
		printf("</td>\n");
	else
		printf("%+0.3f</td>\n", ($left->net + $right->net - (2*$dial)));

	echo "<td rowspan=2>";
	if (($newdial != $dial) && ($newdial != 0.0))
		printf("%0.3f", $newdial);
	echo "</td>\n";
}

