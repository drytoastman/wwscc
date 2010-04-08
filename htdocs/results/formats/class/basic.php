<?php

function classResults($results)
{
	global $isMobile;

	if ($results->event->courses > 1)
	{
		$colcount = 8 + $results->event->runs;
		$rowspan = "rowspan='{$results->event->courses}'";
	}
	else
	{
		$colcount = 7 + $results->event->runs;
		$rowspan = "";
	}

	echo("<table class='classresults'>\n"); 
	foreach ($results->classes as $clsresults)
	{
		echo "<tr>\n";
		echo "<th class='classhead' colspan='$colcount'>\n";
		echo "<a name='{$clsresults->class->code}'>{$clsresults->class->code}</a> - {$clsresults->class->descrip}\n";
		echo "</th>\n";
		echo "</tr>\n";

		echo "<tr>\n";
		echo "<th class='pos'></th>\n";
		echo "<th class='trophy'></th>\n";
		echo "<th class='name'>Name</th>\n";

		if (!$isMobile)
		{
			echo "<th class='carnum'>#</th>\n";
			echo "<th class='caryear'>Year</th>\n";
			echo "<th class='cardesc'>Make/Model</th>\n";
		}

		for ($ii = 1; $ii <= $results->event->runs; $ii++)
			echo "<th class='run'>Run$ii</th>\n";
	
		if ($results->event->courses > 1)
			echo "<th class='total'>Total</th>\n";
	
		echo "<th class='points'>Points</th>\n";
		echo "</tr>\n";

		foreach ($clsresults->entrants as $entrant)
		{

			for ($jj = 1; $jj <= $results->event->courses; $jj++)
			{
				echo "<tr>\n";
	
				# For multi course events, only print this info on one row
				if ($jj == 1)
				{	
					echo "<td class='pos' $rowspan>{$entrant->position}</td>\n";
					echo "<td class='trophy' $rowspan>{$entrant->trophy}</td>\n";
					echo "<td class='name' $rowspan>{$entrant->fullname()}</td>\n";
					
					if (!$isMobile)
					{
						echo "<td class='carnum' $rowspan>{$entrant->number}</td>\n";
						echo "<td class='caryear' $rowspan>{$entrant->year}</td>\n";
						echo "<td class='cardesc' $rowspan>{$entrant->desc()} {$entrant->indexstr($clsresults->class)}\n";
					}
				}
	
				# Print runs for course JJ
				for ($ii = 1; $ii <= $results->event->runs; $ii++)
				{
					$run = $entrant->runs[$jj][$ii];
					if ($run != null)
					{
						$marker = '';
						if ($run->norder == 1)
							$marker = 'bestnet';
						else if ($run->iorder == 1)
							$marker = 'bestraw';
	
						echo "<td class='run $marker'>";
	
						if ($run->status == "OK")
							printf("<span class='net'>%0.3f (%d,%d)</span>", $run->net, $run->cones, $run->gates);
						else
							printf("<span class='net'>%s</span>", $run->status);
	
						if ($clsresults->class->isindexed)
							printf("<span class='raw'>[%0.3f]</span>", $run->raw);
					
						echo "</td>\n";
					}
					else
					{
						echo "<td class='run'>-</td>\n";
					}
				}
	
				# Again on multi course, only print this on one row
				if ($jj == 1)
				{
					if ($results->event->courses > 1)
						printf("<td class='total' $rowspan>%0.3f</td>\n", $entrant->sum);
					printf("<td class='points' $rowspan>%0.3f</td>\n", $entrant->points);
				}
	
	
				echo "</tr>\n";
			}
		}
	}
	echo "</table>\n";
}

?>
