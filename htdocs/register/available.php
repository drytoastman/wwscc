<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function availableList($classcode)
{
	if (empty($classcode))
	{
		echo "<h3>No class was selected.  Cannot print a list of available numbers</h3>\n";
		return;
	}
	
	$num = getps("select distinct number from cars where classcode=? and driverid!=?");
	$numbers = $num->loadList(array($classcode, $_SESSION[dbname()]['driverid']));
	$match = array();
	foreach ($numbers as $num)
	{
		$match[$num] = 1;
	}
	
echo "
<script>
function sn(num)
{
	opener.document.getElementById('number').value = num;
	opener.document.getElementById('displaynumber').innerHTML = num;
	window.close();
}
</script>
<style>
ul {
float: left;
margin: 0;
padding: 0;
list-style: none;
width: 400px;
}

li {
text-align: right;
font-size: 0.9em;
font-family: arial;
color: #EDD;
float: left;
width: 40px;
margin: 0;
padding: 0;
}

a {
text-decoration: none;
color: blue;
}
</style>

<h3>Your Available Numbers For $classcode - Select One</h3>
<ul>
";
	
	for ($num = 0; $num < 2000; $num++)
	{
		if ($match[$num])
			echo "<li>$num</li>\n";
		else
			echo "<li><a href='#' onclick='sn($num);'>$num</a></li>\n";
	}
	echo "</ul>\n";
}

?>
