<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function transfer_process($type, $dbname)
{
	$encrypted = trim(file_get_contents('series/password'));
	$password = $_SERVER['HTTP_X_SCOREKEEPER'];

	if (crypt($password, $encrypted) == $encrypted)
	{
		if ($type == 'download')
			download($dbname, true);
		else if ($type == 'copy')
			download($dbname, false);
		else if ($type == 'upload')
			upload($dbname);
		else if ($type == 'available')
			available();
		else
			header('HTTP/1.1 403 Forbidden');
	}
	else
	{
		header('HTTP/1.1 403 Forbidden');
	}
}

function available()
{
	$list = dbfiles();
	foreach ($list as $file)
	{
		print "$file\n";
	}
}

function download($fullname, $lock = true)
{
	$name = basename($fullname, ".db");
	$path = dbpath($name);
	if (!file_exists($path))
	{
		header('HTTP/1.1 404 Not Found');
		return;
	}

	if ($lock)
	{
		/* 'Lock' the database and perform a internal cleanup on it before downloading. */
		$myDB = new Database($name);
		$dbh = $myDB->dbh;
		$dbh->exec("begin");
		$dbh->exec("update settings set valuestr='1' where keystr='locked'");
		$dbh->exec("end");
		$dbh->exec("vacuum");
		$myDB->dbh = null;
	}

	$fp = fopen($path, 'rb');
	header("Content-Length: " . filesize($path));
	fpassthru($fp);
}


function upload($fullname)
{
	$name = basename($fullname, ".db");
	if (move_uploaded_file($_FILES['db']['tmp_name'], dbpath($name)))
		header('HTTP/1.1 200 OK');
	else
		header('HTTP/1.1 403 Forbidden');
}

?>
