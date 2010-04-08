<?php

function auditreport($data, $numruns, $order)
{
	echo "
	<style>
	table.auditreport td, table.auditreport th { border: 1px solid #AAA; padding: 5px; }
	table.auditreport .bold { font-weight: bold; }
	</style>

	<table class='auditreport'>
	<thead><tr>
	<th>First</th>
	<th>Last</th>
	<th>#</th>
	<th>Cls</th>
	";

	for ($ii = 1; $ii <= $numruns; $ii++)
		echo "<th>Run$ii</th>\n";

	echo "
	</tr>
	</thead>
	<tbody>
	";

	if ($order == 'lastname') $lclass = 'bold';
	if ($order == 'firstname') $fclass = 'bold';

	foreach ($data as $entrant)
	{
		printf("<tr>\n");
		printf("<td class='%s'>%s</td>\n", $fclass, substr($entrant->firstname, 0, 8));
		printf("<td class='%s'>%s</td>\n", $lclass, substr($entrant->lastname, 0, 8));
		printf("<td>%d</td>\n", $entrant->number);
		printf("<td>%s (%s)</td>\n", $entrant->classcode, $entrant->indexcode);

		for ($ii = 1; $ii <= $numruns; $ii++)
		{
			$run = $entrant->runs[$ii];
			if ($run != null)
			{
				echo "<td>";
				if ($run->status == "OK")
				{
					printf("%0.3f ", $run->raw + ($run->cones*2) + ($run->gates*10));
					printf("(%d,%d)", $run->cones, $run->gates);
				}
				else
					printf("%s", $run->status);

				echo "</td>\n";
			}
			else
			{
				echo "<td></td>\n";
			}
		}
		
		printf("</tr>\n");
	}

	echo "
	</tbody>
	</table>
	";
}


?>
