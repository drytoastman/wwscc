<?php

/*
 * This software is licensed under the GPLv3 license, included as
 * ./GPLv3-LICENSE.txt in the source distribution.
 *
 * Portions created by Brett Wilson are Copyright 2008 Brett Wilson.
 * All rights reserved.
 */



function settingsForm($prefix)
{
    echo "
    <h3>Series Settings</h3>
    <form action='$prefix/post/settings' method='POST'>
	<input type='hidden' name='destination' value='{$_SERVER['REQUEST_URI']}'>
    <table class='input'>
	";

	$s = loadSettings();
		
	echo "<tr><th colspan=2>Registration</th></tr>\n";
	foreach (array("sponsorlink", "sponsorimage", "seriesimage", "cardimage") as $k)
	{
		echo "<tr><th>$k</th><td><input type='text' size=30 name='register_$k' value='".esc($s["register_$k"])."'></td></tr>\n";
	}

	echo "
	<tr><th colspan=2>Results</th></tr>
	<tr><th>Class Format</th><td>" .
		fileSelect('results/formats/class/', 'results_xformat', $s['results_xformat']) .  "
	</td></tr>
	<tr><th>Audit Format</th><td>".
		fileSelect('results/formats/audit/', 'results_aformat', $s['results_aformat']) . "
	</td></tr>
	<tr><th>Event Format</th><td>
		Class " . fileSelect('results/formats/class/', 'results_ecformat', $s['results_ecformat']) . "
		Template " . fileSelect('results/formats/event/', 'results_etformat', $s['results_etformat']) . "
	</td></tr>
	<tr><th>Champ Format</th><td>".
		fileSelect('results/formats/champ/', 'results_cformat', $s['results_cformat']) . "
	</td></tr>
	";

	foreach (array("css", "bestof") as $k)
	{
		echo "<tr><th>$k</th><td><input type='text' size=30 name='results_$k' value='".esc($s["results_$k"])."'></td></tr>\n";
	}

	echo "
    </table>
	<br/>
    <input type='submit' value='Update'>
    </form>
    ";
}


function fileSelect($dir, $name, $default)
{
	$html = "<select name='$name'><option></option>\n";
	if ($dh = opendir($dir)) 
	{
		while (($file = readdir($dh)) !== false) 
		{
			if (substr($file, strrpos($file, '.')) != '.php')
				continue;
	#		$html .= "AAA" . strrpos($file, '.') . "ZZZ\n";

			if ($file == $default)
				$html .= "<option selected='selected'>$file</option>\n";
			else
				$html .= "<option>$file</option>\n";
		}
		closedir($dh);
	}
	$html .= "</select>\n";
	return $html;
}

?>
