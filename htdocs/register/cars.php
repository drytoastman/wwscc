<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function carDisplay($prefix, $driverid)
{
	$classes = getClasses();
	$indexes = getIndexes();
	$cars = loadDriverCars($driverid);

	echo "
	<h2>My Cars</h2>
	<div class='infobox'>
	<ul>
	<li>To register one of these cars for an event, use the <a href='$prefix/events'>Events</a> tab to the left</li>
	</ul>
	</div>

	<form name='carform' id='carform' action='$prefix/post/car' method='post'>
	<div id='carsdisplay' style='display:block;'>
	<table class='carlist'>
	<tbody>
	";

	$inuse = array();
	$notinuse = array();
	foreach ($cars as &$car)
	{
		if ($car->inuse)
			$inuse[] = $car;
		else
			$notinuse[] = $car;
	}

	echo "<tr><th colspan='4'>Cars In Use</th></tr>\n";
	echo "<tr><td class='info' colspan='4'>Cars in use can only have their description changed</td></tr>\n";
	foreach ($inuse as $car)
		carRow($car);

	echo "<tr><th colspan='4'>Unused Cars</th></tr>\n";
	echo "<tr><td class='info' colspan='4'>Cars not in use can be fully modified or deleted</td></tr>\n";
	foreach ($notinuse as $car)
		carRow($car);

	echo "
	</tbody>
	</table>
	<input class='addnew' type='button' name='create' value='Create New Car' onclick='newcar();'/>
	</div>

	<div id='careditor' style='display:none;'>
	<input type='hidden' id='carid' name='carid' value=''/>
	<input type='hidden' id='ctype' name='ctype' value=''/>
	<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}'/>
	<table class='careditor'>
	<thead>
	<tr><th colspan='2'>Car Editor</th></tr>
	</thead>

	<tbody>
	<tr><th>Year</th>  <td><input id='year'   name='year'   type='text'/></td></tr>
	<tr><th>Make</th>  <td><input id='make'   name='make'   type='text'/></td></tr>
	<tr><th>Model</th> <td><input id='model'  name='model'  type='text'/></td></tr>
	<tr><th>Color</th> <td><input id='color'  name='color'  type='text'/></td></tr>

	<tr><th>Class</th> <td>
	<select id='classcode' name='classcode' onchange='classchange();'>
	";

	foreach ($classes as $c)
	{
		if ($c->isindexed)
			echo "\t<option indexed='1'>{$c->code}</option>\n";
		else
			echo "\t<option>{$c->code}</option>\n";
	}

	echo "
	</select>
	</td></tr>

	<tr><th>Index</th> <td>
	<select id='indexcode' name='indexcode'>
	<option></option>
	";

	foreach ($indexes as $code => $value)
	{
		echo "\t<option>$code</option>\n";
	}

	echo "
	</select>
	</td></tr>
	<tr><th>Number</th><td>
		<input id='number' name='number' type='hidden'/> 
		<span id='displaynumber'></span>
		<span id='numselector'><a id='availablelink' href='available/' target='numberselection'>Select Number</a></span>
		</td></tr>
	</tbody>
	</table>

	<br/>
	<input type='submit' id='submitbutton' onclick='return checkRegForm();' value='OK' />&nbsp;
	<input type='button' value='Cancel' onclick='switchtocars();' />
	<br/>
	</div>
	</form>
	";
}

function carRow($car)
{
	echo "
	<tr>
	<td>{$car->desc()}</td>
	<td>{$car->clsstring()}</td>
	<td>{$car->number}</td>
	";

	if ($car->inuse)
	{
		echo "
		<td><input type='button' value='update description' onclick=\"update({$car->csvstring()});\" /></td>
		";
	}
	else
	{
		echo "
		<td>
		<input type='button' value='modify' onclick=\"modify({$car->csvstring()});\" />
		<input type='button' value='delete' onclick=\"del('carform', {$car->csvstring()});\" />
		</td>
		";
	}
	echo "</tr>\n";
}

?>
