<?php
/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */

function sql_process($type, $dbname)
{
	global $myDB;

	//$encrypted = trim(file_get_contents('series/password'));
	$password = $_SERVER['HTTP_X_SCOREKEEPER'];

	try
	{
		$myDB->dbh->beginTransaction();
		$in = fopen("php://input", "r");
		$printid = false;
		while (1)
		{
			$head = trim(fgets($in));
			if (empty($head))
				break;
				
			switch ($head)
			{
				case 'BEGIN SELECT':
					parseSelect($in);
					$printid = false;
					break;
				case 'BEGIN UPDATE':
				case 'BEGIN GROUPUPDATE':
					parseUpdate($in);
					$printid = true;
					break;
				default:
					throw new Exception("Bad request type $head");
			}
		}
		
		if ($printid)
			print "LASTID " . lastid() . "\n";
		$myDB->dbh->commit();
	}
	catch (Exception $e)
	{
		$myDB->dbh->rollBack();
		print "ERROR";
		print $e;
	}
}

function parseUpdate($in)
{
	$sql = rtrim(fgets($in));
	while (1)
	{
		$argstr = trim(fgets($in));
		if ($argstr == "END UPDATE")
			return;
		else if ($argstr == "")
			$args = null;
		else
			$args = explode(",", $argstr);

		$stmt = getps($sql)->prep;
		$stmt->execute($args);
	}
}

function parseSelect($in)
{
	$sql = rtrim(fgets($in));
	$argstr = trim(fgets($in));
	$end = trim(fgets($in));

	if ($argstr == "")
		$args = null;
	else
		$args = explode(",", $argstr);

	$stmt = getps($sql)->prep;
	$stmt->execute($args);

	print "RESULTS\n";

	$names = array();
	try
	{
		for ($ii = 0; $ii < $stmt->columnCount(); $ii++)
		{
			$meta = $stmt->getColumnMeta($ii);
			$names[$ii] = $meta['name'];
		}
	}
	catch (Exception $e)
	{
	}

	print implode(",", $names) . "\n";
	while ($arr = $stmt->fetch(PDO::FETCH_NUM))
	{
		print implode(",", $arr) . "\n";
	}

	$stmt->closeCursor();
}


?>
