<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function loginForm($prefix)
{
	echo "
	<form name='loginform' id='loginform' action='$prefix/post/login' method='post' onsubmit='return checkform();'>
	<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}'/>
	
	<h3>To register enter the following</h3>
	<table class='login'>
	<tr><td>First Name</td><td><input name='firstname' id='firstname' size='20' value='' type='text'/></td></tr>
	<tr><td>Last Name</td><td><input name='lastname' id='lastname' size='20' value='' type='text'/></td></tr>
	<tr><td>Email</td><td><input name='email' id='email' size='20' value='' type='text'/></td></tr>
	</table>

	<input type='submit' name='Submit' value='Submit'/>

	</form>

	";
}


?>
