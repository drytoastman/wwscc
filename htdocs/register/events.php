<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function format_car($cptr)
{
	return "{$cptr->classcode}/{$cptr->number} - {$cptr->year} {$cptr->make} {$cptr->model} {$cptr->color}";
}

function paypalLink($prefix, $event)
{
	echo "
	<form style='display:inline;' action='https://www.paypal.com/cgi-bin/webscr' method='post' target='_blank'>
	<span class='eimage'>
	<input type='hidden' name='cmd' value='_xclick' />
	<input type='hidden' name='business' value='{$event->paypal}' />
	<input type='hidden' name='item_name' value='".esc($event->name)."' />
	<input type='hidden' name='custom' value='{$event->id}.{$_SESSION[dbname()]['driverid']}' />
	<input type='hidden' name='amount' value='{$event->cost}' />
	<input type='hidden' name='currency_code' value='USD' />
	<input type='hidden' name='notify_url' value='http://{$_SERVER["SERVER_NAME"]}$prefix/ipn'>
	<input type='image' src='https://www.paypal.com/en_US/i/btn/x-click-but3.gif' name='submit'
						alt='Make payments with payPal - it&#039;s fast, free and secure!' />
	</span>
	</form>
	";
}


function carSelection($poststr, $cars, $disabled, $selected = 0)
{
	echo "<form action='$poststr' method='post'>\n";
	echo "<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}' />\n";
	echo "<input type='hidden' name='carid' value='0' />\n";
	echo "<select class='eselector' name='selectcarid' onchange='submitAndWait(this, \"selectcarid\");'>\n";
	if ($selected == 0)
		echo "<option selected='selected' value='-1'></option>\n";
	else
		echo "<option value='-1'>--- unregister this entry ---</option>\n";

	foreach ($cars as $car)
	{
		if ($car->id == $selected)
			echo "<option selected='selected' value='{$car->id}'>".format_car($car)."</option>\n";
		else if (array_search($car->id, $disabled) === FALSE)
			echo "<option value='{$car->id}'>".format_car($car)."</option>\n";
	}
	echo "</select>\n";
	echo "</form>\n";
}


function eventDisplay($prefix, $driverid)
{
	$events = getevents();
	$cars = loadDriverCars($driverid);

	echo "
	<h2>Series Events</h2>
	<div class='infobox'>
	<ul>
	<li>To register a car for an event, select a car from the drop down menu</li>
	<li>To change/unregister a car, select a different car or delete from the appropriate drop down menu</li>
	<li>To create a new car, edit or delete cars, click the <a href='$prefix/cars'>My Cars</a> tab to the left</li>
	</ul>
	</div>
	<table class='eventlist'>
	<tr>
	<th>Event</th>
	<th>Registration</th>
	</tr>
	";
	//<li>If the event is already full, you will be placed on a wait list</li>

	foreach ($events as $event)
	{
		$closed = $event->regclosed();
		$opened = $event->regopened();
		$tdclass = '';
		if ($closed || !$opened)
			$tdclass = 'closed';

		$curCount = loadEventCount($event->id);

		if (!$opened)
			$closedstr = "Has not opened yet";
		else
			$closedstr = date('M j \a\t h:iA T', $event->regclosed/1000);

		/***** Event Info cell ***********/

		echo "
		<tr>
		<td class='$tdclass' valign='top'>
			<a class='elink' href='$prefix/viewentries/{$event->id}'>{$event->name}</a>
			<span class='elabel'>Date:</span>
			<span class='evalue'>{$event->datestr()}</span><br/>
			<span class='elabel'>Closes:</span>
			<span class='evalue'>$closedstr</span><br/>
		";

		if (!empty($event->host))
		{
			echo "<span class='elabel'>Host:</span>\n";
			echO "<span class='evalue'>{$event->host}</span><br/>\n";
		}

		if (!$closed && $opened)
		{
			if (!empty($event->totlimit))
			{
				echo "<span class='elabel'>Limit:</span>\n";
				echo "<span class='evalue'>$curCount / {$event->totlimit}</span><br/>\n";
			}

			if (!empty($event->cost))
			{
				echo "<span class='elabel'>Cost:</span>\n";
				echo "<span class='evalue'>{$event->cost}</span><br/>\n";
			}

			if (!empty($event->paypal))
			{
				echo "<span class='elabel'>Paypal:</span>\n";
				echo paypalLink($prefix, $event);
			}

			if (!empty($event->snail))
			{
				echo "<span class='elabel'>Mail:</span>\n";
				echo "<div class='eaddress'>{$event->snail}</div>\n";
			}

			if (!empty($event->notes))
			{
				echo "<span class='elabel'>Notes:</span>\n";
				echo "<div class='enotes'>{$event->notes}</div>\n";
			}
		}

		echo "
		</td>
		<td class='$tdclass'>
		";


		/***** Car selection cell ***********/

		$regentries = loadDriverList($event->id, $driverid);
		$payments = loadPaymentList($event->id, $driverid);

		if ($closed || !$opened)
		{
			foreach ($regentries as $reg)
				echo format_car($cars[$reg]) . "<br>\n";
		}
		else
		{
			/* Where they add a new registration */
			echo "<div class='erule'>Register a car from <a href='$prefix/cars'>My Cars</a></div>\n";
			if (!empty($event->totlimit) && ($curCount >= $event->totlimit))
				echo "<span class='limit'>This event's prereg limit of {$event->totlimit} has been met.</span>";
			else if (count($regentries) >= $event->perlimit)
				echo "<span class='limit'>You have reached this event's prereg limit of {$event->perlimit} car(s).</span>";
			else
				carSelection("$prefix/post/register/{$event->id}/add", $cars, $regentries);


			/* Where they change their registration */
			if (count($regentries) > 0)
			{
				echo "<div class='espacer'></div>\n";
				echo "<div class='erule'>Change/Unregister a currently registered car</div>\n";
				foreach ($regentries as $regid => $reg)
				{
					//echo "$regid:";
					carSelection("$prefix/post/register/{$event->id}/chg/$regid", $cars, $regentries, $reg);
				}
			}

			/* Just information payment information */
			if (count($payments) > 0)
			{
				echo "<div class='espacer'></div>\n";
				echo "<div class='erule'>Paypal Payments</div>\n";
				foreach ($payments as $p)
				{
					echo "\${$p->amount} ({$p->status})<br>\n";
				}
			}

		}
		echo "</td>\n";
		echo "</tr>\n";
	}

	echo "
	</table>
	";
}

