<?php

function pointssort($a, $b)
{
	return ($b['points']['total'] * 1000) - ($a['points']['total'] * 1000);
}

function champReport($data, $events, $classes)
{
	printf("<table class='champ'>\n");

	foreach ($classes as $c)
	{
		if (!$c->ctrophy) # No champ trophies for this class
			continue;

		if (empty($data[$c->code]))  # No data for this class
			continue;

		$people = $data[$c->code];

		printf("<tr><th class='classhead' colspan='%d'>%s - %s</th></tr>\n", (4+count($events)), $c->code, $c->descrip);
		echo "<tr>\n";
		echo "<th>#</th>\n";
		echo "<th>Name</th>\n";
		echo "<th>Attended</th>\n";
		$ii = 0;
		foreach ($events as $eventid => $e)
		{
			$ii++;
			echo "<th>Event $ii</th>\n";
		}
		echo "<th>Total</th>\n";
		echo "</tr>\n";
		

		usort($people, "pointssort");

		$ii = 0;
		foreach ($people as $plist) 
		{
			$ii++;
			echo "<tr>\n";
			echo "<td>$ii</td>\n";
			echo "<td class='name'>{$plist['name']}</td>\n";
			echo "<td class='attend'>{$plist['events']}</td>\n";
			foreach ($events as $eventid => $e)
			{
				$value = $plist['points'][$eventid];
			
				if (!empty($value))
				{
					/*
					if (($value == $pointlist['drop1']) || ($value == $pointlist['drop2']))
						$tdclass = 'drop';
					else if ($value == $pointlist['last'])
						$tdclass = 'last';
					else
						$tdclass = '';
					*/
				
					printf("<td class='points %s'>%0.3lf</td>\n", $tdclass, $value);
				}
				else
					echo "<td class='points drop'></td>\n";
			}
			printf("<td class='points'>%0.3lf</td>\n", $plist['points']['total']);
			echo "</tr>\n";
		}
	}
	
	printf("</table>");
}
	
?>
