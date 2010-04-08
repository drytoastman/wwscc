<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function indexPage($event, $active, $challenges = null) 
{
global $rootpath;
global $dbname;

echo "
<style> ul, ol, h3 { text-align: left; } </style>
<h3><a href='$rootpath/announcer/$dbname/{$event->id}/'>Announcer Panel</a></h3>

<h3>Select A Class</h3>
<ul class='classlist'>
";
foreach ($active as $class) 
	echo "<li><a href='class?list=$class'>$class</a></li>\n";
echo "
</ul>
<br style='clear:left;'>
";

if ($event->ispro) 
{
	echo "
	<h3>Challenges</h3>
	<ul class='challist'>
	";
	foreach ($challenges as $ch)
		echo "<li><a href='challenge/{$ch->id}/'>{$ch->name}</a></li>\n";
	echo "
	</ul>
	<br style='clear:left;'>
	";
}


echo "
<h3>Classes Running In ...</h3>
<ul class='challist'>
";
for ($ii = 1; $ii <= 6; $ii++)
	echo "<li><a href='group?course=1&list=$ii'>Run Group $ii</a></li>\n";
echo "
</ul>
<br style='clear:left;'>
";


echo "
<h3>Other Stuff</h3>
<ol>
<li><a href='topindex/'>Top Indexed Times</a></li>
<li><a href='topraw/'>Top Raw Times</a></li>
<li><a href='all/'>All Classes</a></li>
<li><a href='post/'>Official Listing</a></li>
<li><a href='../champ/'>Championship</a></li>
";
if ($event->ispro) 
	echo "<li><a href='dialins/'>Dialins</a></li>\n";
echo "
</ol>
";

}

?>
