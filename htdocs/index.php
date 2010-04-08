<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */


session_start();

# We buffer here incase someone downstream decides to override the output
ob_start();
require_once('lib.php');
require_once('checkMobile.php');

try
{
	$prefix = array();
	$command = explode('/', $_SERVER['REQUEST_URI']);
	$rootpath = explode('/', $_SERVER['SCRIPT_NAME']);

	/* Remove any initial path to get to our stuff */
	array_pop($rootpath);  // remove index.php
	for ($ii = 0; $ii < count($rootpath); $ii++)
		array_push($prefix, array_shift($command));


	# Convert rootpath array back into a URL string, extract any section or db args
	$rootpath = implode('/', $rootpath);
	$section = array_shift($command);
	$dbname = array_shift($command);

	array_push($prefix, $section, $dbname);
	$startpath = implode('/', $prefix);

	# Remove any GET junk from the end of the last command, so we can get the command
	$stridx = count($command) - 1;
	$chridx = strpos($command[$stridx], "?");
	if (!($chridx === FALSE))
		$command[$stridx] = substr($command[$stridx], 0, $chridx);


	if ($section == '')
	{
		header('Location: results');
		exit;
	}

	global $isMobile;
	$isMobile = isMobileDevice();

	/***

	At this point, we have the following variables:

	rootpath:  the URL path that leads to our root dir, i.e. where we (index.php) are in the virtual host
	section:   (results, admin, resgiter)
	dbname:    the name of the database file we are interested in
	startpath: the URL path that includes the section and database, reference for section links
	command:   an array of command pieces for the section/database (take from the URL)

	Example: 

	http://localhost/subdir/results/slush2008/1/class?list=TOPM

	rootpath = '/subdir'
	section = 'results'
	dbname = 'slush2008'
	startpath = '/subdir/results/slush2008'
	command = ['1', 'class']

	***/

	switch ($section)
	{
		case 'results':
			require_once("results/process.php");
			database_open($dbname);
			results_process($rootpath, $startpath, $dbname, $command);
			dbclose();
			break;
	
		case 'announcer':
			require_once("announcer/process.php");
			database_open($dbname);
			announcer_process($rootpath, $startpath, $dbname, $command);
			dbclose();
			break;
			
		case 'register':
			require_once("register/process.php");
			database_open($dbname);
			html_start("Scorekeeper Registration", array("register.css"), array("register.js"));
			register_process($startpath, $command);
			dbclose();
			html_end();
			break;

		case 'admin':
			require_once("admin/process.php");
			database_open($dbname);
			html_start("Scorekeeper Administration", array("admin.css", "adminmenu.css"), array("admin.js", "sortabletable.js"));
			admin_process($startpath, $command);
			dbclose();
			html_end();
			break;

		case 'sql':
			require_once("sql.php");
			database_open($dbname);
			sql_process($section, $dbname);
			dbclose();
			break;

		case 'json':
			require_once("json/process.php");
			database_open($dbname);
			json_process($startpath, $command);
			dbclose();
			break;

		case 'download':
		case 'upload':
		case 'copy':
		case 'available':
			require_once("transfer.php");
			ob_end_clean();  # Definatly don't buffer this part
			transfer_process($section, $dbname);
			exit;

		default:
			print "Nothing to see here, move along.\n";
			break;
	}
}
catch (Exception $e)
{
	print "<pre>error in data: $e</pre>";
}

ob_end_flush();  # If we got there far, spit out the buffer


###########################################################################

function html_start($title, $csslist = array(), $jslist = array())
{
	global $rootpath;

	echo "
<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>
<html xmlns='http://www.w3.org/1999/xhtml'>
<head>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
<title>$title</title>
";

	foreach ($jslist as $js) echo "<script type='text/javascript' src='$rootpath/js/$js'></script>\n";
	foreach ($csslist as $css) echo "<link href='$rootpath/css/$css' rel='stylesheet' type='text/css' />\n";

	echo "
</head>
<body>
";
}

function html_end()
{
	echo "</body></html>\n";
}

function database_open($name)
{
	if (empty($name))
	{
		echo "<h3>Select series:</h3>\n";
		echo "<ol>\n";
		foreach (glob(dbpath('*')) as $file)
		{
			$name = basename($file, ".db");
			echo "<li><a href='$name/'>$name</a></li>\n";
		}
		echo "</ol>\n";
		echo "</body>\n";
		echo "</html>\n";
		exit;
	}

	dbopen($name);
}


?>
