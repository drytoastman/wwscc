<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



	# Where the database resides, no longer different between systems
	function dbpath($name)
	{
		return "series/$name.db";
	}

	function dbfiles()
	{
		chdir("series");
		$list = glob("*.db");
		chdir("..");
		return $list;
	}

	class Database 
	{
		function __construct($name)
		{
			$this->name = $name;

			$file = dbpath($name);
			if (file_exists($file))
			{
				$this->dbh = new PDO("sqlite:$file"); //, null, null, array(PDO::ATTR_PERSISTENT => true));
				$this->dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

				# Not using persistent connections
				# 1. Not available on dreamhost as its run PHP as a CGI
				# 2. Interferes with backup/movement of files on data entry machine
			}
			else
			{
				throw new Exception("No such database $file");
			}

			$this->preplist = array();
		}

		function name()
		{
			return $this->name;
		}

		function getps($sql)
		{
			$key = md5($sql);
			if (!array_key_exists($key, $this->preplist))
				$this->preplist[$key] = new PrepWrapper($this->dbh->prepare($sql));
			return $this->preplist[$key];
		}

		function closeAll()
		{
			foreach ($this->preplist as $prep)
				$prep->close();
		}

		function lastid()
		{
			return $this->dbh->lastInsertId();
		}
	}


	class PrepWrapper
	{
		function __construct($prep)
		{
			$this->prep = $prep;
		}

		## Simple pass thru
		function execute($param = null)
		{
			return $this->prep->execute($param);
		}

		## Another pass through
		function close()
		{
			return $this->prep->closeCursor();
		}

		## Return the first column of the first row
		function loadAValue($param = null)
		{
			$this->prep->execute($param);
			return $this->prep->fetchColumn();
		}

		## Return the first line of data and create an object from it
		function loadOne($classname, $param = null)
		{
			$this->prep->execute($param);
			return $this->prep->fetchObject($classname);
		}

		## Return the first column of the results and return it as an array
		function loadList($param = null)
		{
			$this->prep->execute($param);
			return $this->prep->fetchAll(PDO::FETCH_COLUMN, 0);
		}

		## Return an array of objects made from the results
		function loadArray($classname, $param = null)
		{
			$this->prep->execute($param);
			return $this->prep->fetchAll(PDO::FETCH_CLASS, $classname);
		}

		## Return an array of objects from the results, indexed by one of the attributes
		function loadIndexArray($classname, $index, $param = null)
		{
			$this->prep->execute($param);
	
			$ret = array();
			while ($obj = $this->prep->fetchObject($classname))
			{
				$ret[$obj->$index] = $obj;
			}

			$this->prep->closeCursor();
			return $ret;
		}

		## Return a named hash using columns key1 and key2
		function loadPairs($key1, $key2, $param = null)
		{
			$this->prep->execute($param);
	
			$ret = array();
			while ($obj = $this->prep->fetch())
			{
				$ret[$obj[$key1]] = $obj[$key2];
			}
	
			$this->prep->closeCursor();
			return $ret;
		}

	}

	/* Helpers for 'globalizing' the database variable */

	function dbopen($name)
	{
		global $myDB;
		$myDB = new Database($name);
	}

	function dbname()
	{
		global $myDB;
		return $myDB->name();
	}

	function dbclose()
	{
	}

	function getps($sql)
	{
		global $myDB;
		return $myDB->getps($sql);
	}

	function lastid()
	{
		global $myDB;
		return $myDB->lastid();
	}



	class Event
	{
		function datestr()
		{
			return date("M j Y", $this->date/1000);
		}

		function regopened()
		{
			return ($this->regopened/1000 < time());
		}

		function regclosed()
		{
			return ($this->regclosed/1000 < time());
		}

		function label()
		{
			return "{$this->name} - {$this->datestr()}";
		}
	}


	class EventResults
	{
		function __construct()
		{
			$this->classes = array();
		}
	}

	class ClassResults
	{
		function __construct()
		{
			$this->entrants = array();
			$this->class = null;
		}
	}

	class Challenge
	{
	}

	class ChallengeRound
	{
	}

	class TopRawTimes
	{
	}

	class TopNetTimes
	{
	}

	class Entrant
	{
		function desc()
		{
			return "{$this->make} {$this->model} {$this->color}";
		}

		function fullname()
		{
			return "{$this->firstname} {$this->lastname}";
		}

		function indexstr($c)
		{
			if (($c->isindexed) && ($c->clsindex != 1.0))
				return "({$this->indexcode}*)\n";
			else if ($c->isindexed)
				return "({$this->indexcode})\n";
			else
				return "";
		}
	}


	class Driver
	{
		function fullname()
		{
			return "{$this->firstname} {$this->lastname}";
		}
	}


	class Car
	{
		function clsstring()
		{
			if (!empty($this->indexcode))
				return "{$this->classcode} ({$this->indexcode})";
			else
				return "{$this->classcode}";
		}
	
		function desc()
		{
			return "{$this->year} {$this->make} {$this->model} {$this->color}";
		}


		function esc($str)
		{
		}


		function csvstring()
		{
			$ret = '';
			foreach (array('id', 'year', 'make', 'model', 'color', 'classcode', 'indexcode', 'number') as $var)
			{
				$ret .= "'".addcslashes($this->$var, "'")."',";
			}
			return rtrim($ret, ",");
		}
	}


	class RegEntry extends Driver
	{
		function fullname()
		{
			return "{$this->firstname} {$this->lastname}";
		}
	
		function desc()
		{
			$str = "{$this->year} {$this->make} {$this->model} {$this->color} ";
			if (!empty($this->indexcode))
				$str .= "({$this->indexcode})";
			return $str;
		}

		function clsstring()
		{
			if (!empty($this->indexcode))
				return "{$this->classcode} ({$this->indexcode})";
			else
				return "{$this->classcode}";
		}
	}

	class Run
	{
		function results_display()
		{
			return "{$this->net}<br>{$this->raw} ({$this->cones}, {$this->gates})";
		}
	}

	class RunOrder 
	{
		function last3()
		{
		}

		function next3()
		{
		}
	}

	class CarClass {}
	class CarIndex {}
	class Payment {}
	class Result {}




	## Extra utility for extract functions that creates useful sql strings for the caller

	function createArgsList($arg, $keys, $vals)
	{
		switch ($arg)
		{	
			case 'update':
				for ($ii = 0; $ii < count($keys); $ii++)
					$sql[] = "{$keys[$ii]}=?";
				return array($keys, $vals, join(',', $sql));

			case 'insert':
				$marks = array_fill(0, count($keys), '?');
				return array($keys, $vals, join(',', $marks));
				
			default:
				return array($keys, $vals);
		}
	}


	## Extract args from _POST related to the events table

	function extractEventArgs($arg = null)
	{
		$keys = array();
		$vals = array();
	
		foreach (array("ispro") as $k)
		{
			$on = isset($_POST[$k]);
			$keys[] = $k;
			$vals[] = $on ? 1 : 0;
		}

		foreach (array("date", "regopened", "regclosed") as $k)
		{
			$secs = strtotime($_POST[$k]);
			if ($secs === FALSE)
				continue;
	
			$keys[] = $k;
			$vals[] = sprintf("%0.0f", ($secs * 1000));  // sqlite/Java stores in ms
		}
	
		foreach (array("perlimit","totlimit","cost","courses","runs") as $k)
		{
			$val = $_POST[$k];
			if (!is_numeric($val))
				continue;
	
	
			$keys[] = $k;
			$vals[] = $val;
		}
	
		foreach (array("name","location","sponsor","host","chair","designer","paypal","snail","notes") as $k)
		{
			if (empty($_POST[$k]))
				continue;
	
			$keys[] = $k;
			$vals[] = $_POST[$k];
		}
	
		return createArgsList($arg, $keys, $vals);
	}


	## Extract args from _POST related to the drivers table

	function extractDriverArgs($arg = null)
	{
		$keys = array();
		$vals = array();

		foreach (array("firstname", "lastname", "email", "address", "city", "state", "zip", "homephone",
						"workphone", "clubs", "brag", "sponsor", "membernumber") as $k)
		{
			if (empty($_POST[$k]))
				continue;
	
			$keys[] = $k;
			$vals[] = $_POST[$k];
		}
	
		return createArgsList($arg, $keys, $vals);
	}


	## Extract args from _POST related to the cars table

	function extractCarArgs($arg = null)
	{
		$keys = array();
		$vals = array();

		foreach (array("number", "driverid") as $k)
		{
			$val = $_POST[$k];
			if (!is_numeric($val))
				continue;
	
	
			$keys[] = $k;
			$vals[] = $val;
		}

		foreach (array("year", "make", "model", "color", "classcode", "indexcode") as $k)
		{
			if (empty($_POST[$k]))
				continue;
	
			$keys[] = $k;
			$vals[] = $_POST[$k];
		}

		return createArgsList($arg, $keys, $vals);
	}


	/* Some utility functions used by everyone */

	function loadSetting($setting)
	{
		$stmt = getps("select valuestr from settings where keystr=?");
		return $stmt->loadAValue(array($setting));
	}

	function loadSettings()
	{
		$stmt = getps("select * from settings");
		return $stmt->loadPairs("keystr", "valuestr");
	}

	function isLocked()
	{
		$ro = loadSetting('locked');
		return !empty($ro);
	}

	function getEvents()
	{
		$stmt = getps("select * from events order by date"); 
		return $stmt->loadIndexArray("Event", "id");
	}

	function getEvent($eventid)
	{
		$stmt = getps("select * from events where id=?");
		return $stmt->loadOne("Event", array($eventid));
	}

	function getClass($classcode)
	{
		$stmt = getps("select * from classes where code=?");
		return $stmt->loadOne("CarClass", array($classcode));
	}

	function getClasses()
	{
		$stmt = getps("select * from classes order by code");
		return $stmt->loadIndexArray("CarClass", "code");
	}

	function getClassesNumOrder()
	{
		$stmt = getps("select * from classes order by numorder");
		return $stmt->loadIndexArray("CarClass", "numorder");
	}

	function getIndexes()
	{
		$stmt = getps("select code,value from indexes order by code");
		return $stmt->loadPairs('code', 'value');
	}

	function getDriver($driverid)
	{
		$stmt = getps("select * from drivers where id=?");
		return $stmt->loadOne("Driver", array($driverid));
	}

	function getEffectiveIndex($classcode, $indexcode, $alwaysindexed = false)
	{
		$value = 1.0;
		$string = "";

		$gi = getps("select code,value from indexes where code in (?,?)");
		$index = $gi->loadOne("CarIndex", array($classcode, $indexcode));

		$gc = getps("select isindexed,clsindex from classes where code=?");
		$class = $gc->loadOne("CarClass", array($classcode));

		if ($class->isindexed || $alwaysindexed)
		{
			$value *= $index->value;
			$string = $index->code;
		}

		if ($class->clsindex != 1.0)
		{
			$value *= $class->clsindex;
			$string .= "*";
		}

		return array($value, $string);
	}

	function getActiveClasses($eventid)
	{
		$stmt = getps("select distinct r.classcode from eventresults as r, classes as c " .
						"where r.classcode=c.code and r.eventid=? order by c.numorder");
		return $stmt->loadList(array($eventid));
	}

	function event_select($prefix)
	{
		echo "<h2>Select An Event</h2>\n";
		echo "<ol>\n";
		foreach (getEvents() as $event)
			echo "<li><a href='$prefix/{$event->id}/'>{$event->name}</a></li>\n";
		echo "</ol>\n";
	}

	function esc($val) # For escaping strings going into HTML input values
	{
		return htmlspecialchars($val, ENT_QUOTES);
	}

	function blankstr($str)
	{
		if ($str == '') return false;
		if (trim($str) == '') return true;
		return false;
	}
	
	function msToDate($val)
	{
		if ($val == 0)
			return date("m/d/Y");
		else
		    return date("m/d/Y", $val/1000);
	}
	
	function msToTime($val)
	{
		if ($val == 0)
		    return date("m/d/Y H:i");
		else
		    return date("m/d/Y H:i", $val/1000);
	}
	
?>
