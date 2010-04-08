<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function process_ipn()
{
	$_POST['cmd'] = "_notify-validate";

	$curl = curl_init();
	curl_setopt($curl, CURLOPT_URL, "https://www.paypal.com/cgi-bin/webscr");
	curl_setopt($curl, CURLOPT_POST, 1);
	curl_setopt($curl, CURLOPT_POSTFIELDS, $_POST);
	curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
	$response = curl_exec($curl);
	$error = curl_error($curl);
	curl_close ($curl);

	if ($response == 'VERIFIED')
	{
		## txid is a unique key, if a previous exists, this one will overwrite
		## the only time this would concievably happen is when an echeck clears

		$inst = getps("replace into payments (txid,type,date,status,driverid,eventid,amount) values (?,?,?,?,?,?,?)");

  		list($eventid, $driverid) = explode('.', $_POST['custom']);
		$args = array($_POST['txn_id'], $_POST['payment_type'], (time()*1000),
				$_POST['payment_status'], $driverid, $eventid, $_POST['payment_gross']);
		$inst->execute($args);
	}

	return;
}

?>
