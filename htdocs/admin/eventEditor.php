<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function checked($value) { if ($value != 0) return " checked "; else return " "; }

function eventEditor($prefix, $e)
{
	if (empty($e))
	{
		$action = "$prefix/post/createevent";
		$next = "$prefix/";
		$buttonval = "Create";
	}
	else
	{
		$action = "$prefix/{$e->id}/post/editevent";
		$next = "$prefix/{$e->id}/";
		$buttonval = "Update";
	}

    echo "
    <h3>Event Editor</h3>
    <form action='$action' method='POST'>
    <input type='hidden' name='destination' value='$next'>
    <table class='input'>
    <tr><th>Name</th>        <td><input type='text' size=30 name='name'      value='".esc($e->name)."'></td></tr>
    ";

    if (empty($e))
    echo "
    <tr><th>Password</th>    <td><input type='text' size=30 name='password'  value='{$e->password}'></td></tr>
    ";

    echo "
    <tr><th>Date</th>        <td><input type='text' size=30 name='date'      value='".msToDate($e->date)."'></td></tr>
    <tr><th>Location</th>    <td><input type='text' size=30 name='location'  value='".esc($e->location)."'></td></tr>
    <tr><th>Sponsor</th>     <td><input type='text' size=30 name='sponsor'   value='".esc($e->sponsor)."'></td></tr>
    <tr><th>Host</th>        <td><input type='text' size=30 name='host'      value='".esc($e->host)."'></td></tr>
    <tr><th>Designer</th>    <td><input type='text' size=30 name='designer'  value='".esc($e->designer)."'></td></tr>
    <tr><th>Is a Pro</th>    <td><input type='checkbox' name='ispro'". checked($e->ispro) ."></td></tr>
    <tr><th>Courses</th>     <td><input type='text' size=30 name='courses'   value='{$e->courses}'></td></tr>
    <tr><th>Runs</th>        <td><input type='text' size=30 name='runs'      value='{$e->runs}'></td></tr>
    <tr><th>Opens</th>       <td><input type='text' size=30 name='regopened' value='".msToTime($e->regopened)."'></td></tr>
    <tr><th>Closes</th>      <td><input type='text' size=30 name='regclosed' value='".msToTime($e->regclosed)."'></td></tr>
    <tr><th>Person Limit</th><td><input type='text' size=30 name='perlimit'  value='{$e->perlimit}'></td></tr>
    <tr><th>Event Limit</th> <td><input type='text' size=30 name='totlimit'  value='{$e->totlimit}'></td></tr>
    <tr><th>PayPal</th>      <td><input type='text' size=30 name='paypal'    value='".esc($e->paypal)."'></td></tr>
    <tr><th>Mail Address</th><td><input type='text' size=30 name='snail'     value='".esc($e->snail)."'></td></tr>
    <tr><th>Cost</th>        <td><input type='text' size=30 name='cost'      value='{$e->cost}'></td></tr>
    <tr><th>Notes</th>       <td><input type='text' size=30 name='notes'     value='{$e->notes}'></td></tr>
    </table>
    <input type='submit' value='$buttonval'>
    </form>
    ";
}

?>
