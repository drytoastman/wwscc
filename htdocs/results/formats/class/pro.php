<?php

function classResults($results)
{
	global $isMobile;

	if ($results->event->courses > 1)
	{
		$colcount = 6 + $results->event->runs;
		$rowspan = "rowspan='{$results->event->courses}'";
	}
	else
	{
		$colcount = 4 + $results->event->runs;
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
		echo "<th class='pos'>#</th>\n";
		echo "<th class='trophy'>T</th>\n";
		echo "<th class='name'>Entrant</th>\n";

		for ($ii = 1; $ii <= $results->event->runs; $ii++)
			echo "<th class='run'>Run$ii</th>\n";
	
		if ($results->event->courses > 1)
		{
			echo "<th></th>\n";
			echo "<th class='total'>Total</th>\n";
		}
	
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
					echo "<td class='name' align='center' $rowspan>#{$entrant->number} - {$entrant->fullname()}<br/>
							{$entrant->year} {$entrant->desc()} {$entrant->indexstr($clsresults->class)}
					</td>\n";
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
	
						echo "<td class='run $marker'>\n";
	
						if ($run->status == "OK")
							printf("<span class='net'>%0.3f</span>\n", $run->net);
						else
							printf("<span class='net'>%s</span>\n", $run->status);
	
						printf("<span class='raw'>%0.3f (%d,%d)</span>\n", $run->raw, $run->cones, $run->gates);
				
						echo "<span class='reaction'>";	
						if ($run->reaction != 0)
							printf("%0.3f", $run->reaction);
						echo " / ";
						if ($run->sixty > 1.5)
							printf("%0.3f", $run->sixty);
						echo "</span>\n";
	
						echo "</td>\n";
					}
					else
					{
						echo "<td class='run'></td>\n";
					}
				}
	
				# Again on multi course, only print this on one row
				if ($jj == 1)
				{
					if ($results->event->courses > 1)
					{
						echo "<td $rowspan></td>\n"; # fix for css border error in firefox
						printf("<td class='total' $rowspan>%0.3f</td>\n", $entrant->sum);
					}
					printf("<td class='points' $rowspan>%d</td>\n", $entrant->ppoints);
				}
	
				echo "</tr>\n";
			}
		}
	}
	echo "</table>\n";
}

?>
