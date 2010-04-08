<?php

function isMobileDevice()
{
	// check if the user agent value claims to be windows but not windows mobile
	if( stristr($_SERVER['HTTP_USER_AGENT'],'windows') &&
		!stristr($_SERVER['HTTP_USER_AGENT'],'windows ce'))
		return false;

	
	// check if the user agent gives away any tell tale signs it's a mobile browser
	if( eregi('up.browser|up.link|windows ce|iemobile|mini|mmp|symbian|midp|wap|phone|pocket|mobile|pda|psp',
				$_SERVER['HTTP_USER_AGENT']))
		return true;


	// check the http accept header to see if wap.wml or wap.xhtml support is claimed
	if( stristr($_SERVER['HTTP_ACCEPT'],'text/vnd.wap.wml') ||
		stristr($_SERVER['HTTP_ACCEPT'],'application/vnd.wap.xhtml+xml'))
		return true;
	

	// check if there are any tell tales signs it's a mobile device from the _server headers
	if( isset($_SERVER['HTTP_X_WAP_PROFILE']) ||
		isset($_SERVER['HTTP_PROFILE']) ||
		isset($_SERVER['X-OperaMini-Features']) ||
		isset($_SERVER['UA-pixels']))
		return true;


	// build an array with the first four characters from the most common mobile user agents
	$a = array('acs-','alav','alca','amoi','audi','aste','avan','benq','bird','blac','blaz','brew','cell','cldc','cmd-','dang','doco','eric','hipt','inno','ipaq','java','jigs','kddi','keji','leno','lg-c','lg-d','lg-g','lge-','maui','maxo','midp','mits','mmef','mobi','mot-','moto','mwbp','nec-','newt','noki','opwv','palm','pana','pant','pdxg','phil','play','pluc','port','prox','qtek','qwap','sage','sams','sany','sch-','sec-','send','seri','sgh-','shar','sie-','siem','smal','smar','sony','sph-','symb','t-mo','teli','tim-','tosh','tsm-','upg1','upsi','vk-v','voda','w3c ','wap-','wapa','wapi','wapp','wapr','webc','winw','winw','xda','xda-');


	// check if the first four characters of the current user agent are set as a key in the array
	if(isset($a[substr($_SERVER['HTTP_USER_AGENT'],0,4)]))
		return true;

	return false;
}

?>
