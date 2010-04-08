<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function entryList($prefix, $event, $reglist)
{
	echo "<h3>{$event->name}</h3>\n";
	echo "<form method='post' action='$prefix/{$event->id}/post/delreg'>\n";
	echo "<input type='hidden' name='carid' value='-1'>\n";
	echo "<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}'>\n";

	echo "<table class='reglist sortable'>\n";
	echo "<thead>\n";
	echo "<tr>\n";
	echo "<th>Id</th>\n";
	echo "<th class='sorttable_ncalpha'>Name</th>\n";
	echo "<th class='sortable_ncalpha'>Member #</th>\n";
	echo "<th class='sortable_ncalpha'>Email</th>\n";
	echo "<th>Class</th>\n";
	echo "<th>#</th>\n";
	echo "<th>Car</th>\n";
	echo "<th class='sorttable_nosort'></th>\n";
	echo "<th class='sorttable_nosort'></th></tr>\n";
	echo "</thead>\n";
	echo "<tbody>\n";
	foreach ($reglist as $reg)
	{
		echo "<tr>\n";
		echo "<td>{$reg->id}</td>\n";
		echo "<td>{$reg->fullname()}</td>\n";
		echo "<td>{$reg->membernumber}</td>\n";
		echo "<td>{$reg->email}</td>\n";
		echo "<td>{$reg->classcode}</td>\n";
		echo "<td>{$reg->number}</td>\n";
		echo "<td>{$reg->desc()}</td>\n";
		echo "<td><input type='submit' value='unreg' onClick='this.form.carid.value={$reg->id};'></td>\n";
		echo "<td><a href='$prefix/{$event->id}/print?carid={$reg->id}'>Card</a></td>\n";
		echo "</tr>\n";
	}
	echo "</tbody>\n";
	echo "</table>\n";
	echo "</form>\n";
}

?>
