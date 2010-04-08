<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function GetComparisonData()
{
	$stmt = getps("select id, LOWER(firstname) as firstname, " .
					"LOWER(lastname) as lastname " .
					"from drivers order by firstname");
	return $stmt->loadArray("Driver");
}

function GetPairData($id1, $id2)
{
	$stmt = getps("select * from drivers where id in (?,?)");
	return $stmt->loadArray("Driver", array($id1, $id2));
}

function GetCars($id)
{
	$stmt = getps("select * from cars where driverid=?");
	$stmt2 = getps("select COUNT(distinct eventid) from runs where carid=?");

	$list = $stmt->loadArray("Car", array($id));
	foreach ($list as &$c)
	{
		$c->eventcount = $stmt2->loadAValue(array($c->id));
	}
	return $list;
}

function handleDuplicates($commandlist)
{
	$data = GetComparisonData();
	$size = count($data);

	$matches = array();
	for ($ii = 0; $ii < $size; $ii++)
	{
		for ($jj = $ii+1; $jj < $size; $jj++)
		{
			$p1 = $data[$ii];
			$p2 = $data[$jj];

			# Do the first characters of first/lastname at least match?
			if (($p1->firstname[0] == $p2->firstname[0])  
				&& ($p1->lastname[0] == $p2->lastname[0]))
			{
				$levf = levenshtein($p1->firstname, $p2->firstname); // firstname
				$levl = levenshtein($p1->lastname, $p2->lastname); // lastname
	
				$tot = $levf + $levl;
				if ($tot <= 3)
					$matches['name'][$tot][] = array($p1, $p2);
			}
		}
	}

	foreach (array(0, 1, 2, 3) as $matchnum)
	{
		echo "<h2>Match Level $matchnum</h2>\n";
		foreach (array('name') as $type)
		{
			echo "<h3 class='indent1'>By $type</h3>\n";
			echo "<div class='indent2'>\n";
			$set = $matches[$type][$matchnum];
			if ($set != null)
			{
				foreach ($set as $pair)
				{
					echo duplicatelist(GetPairData($pair[0]->id, $pair[1]->id));
					echo "<P>\n";
				}
			}
			echo "</div>\n";
		}
	}
}

function autoid()
{
	return "id".rand();
}

function duplicatelist($people)
{
	$divid = autoid();
	$divid2 = autoid();
	$formid = autoid();
	$formid2 = autoid();
	$id1 = $people[0]->id;
	$id2 = $people[1]->id;

	$cars1 = GetCars($id1);
	$cars2 = GetCars($id2);

$html = "
	<div id='$divid'>
	<form method='POST' id='$formid'>
	<table cellspacing='0' class='mytable sortable'>
	<thead>
	<tr><td>Id</td><td>First</td><td>Last</td><td>Email</td></tr>
	</thead>
	<tbody>" .
	duplicaterow($people[0]) .
	duplicaterow($people[1]) . 
	"</tbody>
	</table>
	<input type='button' value='Merge this group' onClick='switchdisp(\"$divid\", \"$divid2\");'>
	</form>
	</div>
		

	<div id='$divid2' style='display:none;'>
	<form method='POST' id='$formid2'>
	<table class='mytable'>
	<thead>
	<tr><td colspan=2><td align=center>Field<td colspan=2></tr>
	</thead>
	<tbody>
	";

	$fields = array('id', 'firstname', 'lastname', 'email', 'address', 'city', 'state', 'zip',
					'homephone', 'workphone', 'clubs', 'brag', 'sponsor', 'membernumber');
	foreach ($fields as $f)
		$html .= selectionrow($people[0], $people[1], $f);
	foreach ($cars1 as $c)
		$html .= "<tr><td colspan=2>{$c->desc()}({$c->id},{$c->eventcount})</td><td>Check</td><td colspan=2></td></tr>\n";
	foreach ($cars2 as $c)
		$html .= "<tr><td colspan=2></td><td>check</td><td colspan=2>{$c->desc()}({$c->id},{$c->eventcount})</td></tr>\n";

$html .= "
	</tbody>
	</table>
	<input type='button' value='Merge' onClick=\"PostAndUpdate('$formid2', '/post/person/merge/$id1/$id2')\">
	<input type='button' value='Cancel' onClick='switchdisp(\"$divid2\", \"$divid\");'>
	</form>
	</div>

	<br>
";

	return $html;
}


function selectionrow($person1, $person2, $field)
{
	if (($person1->$field == '') && ($person2->$field != ''))
		$rightselect = "checked";	
	else
		$leftselect = "checked";	

	return "
	<tr>
		<td align=right>{$person1->$field}
		<td><input type='radio' name='$field' value='{$person1->$field}' $leftselect>
		<td align=center><b>$field</b>
		<td><input type='radio' name='$field' value='{$person2->$field}' $rightselect>
		<td align=left>{$person2->$field}
	</tr> ";
}


function duplicaterow($person)
{
return  "
		<tr>
		<td><a href='/people/{$person->id}'>{$person->id}</a>
		<td>{$person->firstname}
		<td>{$person->lastname}
		<td>{$person->email}
		</tr>
";
}

