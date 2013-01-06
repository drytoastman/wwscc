<!DOCTYPE html> 
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Live Results Selection</title> 
	<link rel="stylesheet" href="/css/jquery.mobile-1.2.0.min.css" />
	<script src="/js/jquery-1.8.3.min.js"></script>
	<script src="/js/jquery.mobile-1.2.0.min.js"></script>

<style>
.ui-btn-text { font-size: 0.8em; }
td { text-align: center }
button { width: 100%; }


.classname {
	-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
	-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
	box-shadow:inset 0px 1px 0px 0px #ffffff;
	background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #ededed), color-stop(1, #dfdfdf) );
	background:-moz-linear-gradient( center top, #ededed 5%, #dfdfdf 100% );
	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#ededed', endColorstr='#dfdfdf');
	background-color:#ededed;
	-moz-border-radius:6px;
	-webkit-border-radius:6px;
	border-radius:6px;
	border:1px solid #dcdcdc;
	display:inline-block;
	color:#777777;
	font-family:arial;
	font-size:19px;
	font-weight:bold;
	padding:6px 24px;
	text-decoration:none;
	text-shadow:1px 1px 0px #ffffff;
}.classname:hover {
	background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #dfdfdf), color-stop(1, #ededed) );
	background:-moz-linear-gradient( center top, #dfdfdf 5%, #ededed 100% );
	filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#dfdfdf', endColorstr='#ededed');
	background-color:#dfdfdf;
}.classname:active {
	position:relative;
	top:1px;
}


</style>
</head> 

<body> 

<div data-role="page" data-theme="b" >
	<div data-role="header" data-position="fixed">
		<h1>View Selection</h1>
	</div>
	<div data-role="content">

	<button onclick="gotoBrowser();" data-theme="a" >Go</button>
	
	<label><input type='checkbox' name='Any-PAX'/>PAX</label>
	<label><input type='checkbox' name='Any-Raw'/>Raw</label>

	<table>
		<tr><th>Class</th><th>Event</th><th>Champ</th></tr>
		%for cls in c.classes:
		<tr>
			<td><label>${cls.code}</label></td>
			<td><label><input type='checkbox' name='${cls.code}-Event'/>&nbsp;</label></td>
			<td><label><input type='checkbox' name='${cls.code}-Champ'/>&nbsp;</label></td>
			<td><button onclick='quick("${cls.code}");' data-theme="a">Quick</button></td>
		</tr>
		%endfor
	</table>

	<a onclick="gotoBrowser();" >Go</a>
	</div>
</div>

</body>
</html>

<script type='text/javascript'>
function quick(code)
{
	window.location = "${h.url_for(action="browser")}?views=" + code + ",Event," + code + ",Champ";
}

function gotoBrowser()
{
	var p = [];
	$(':checked').each(function() {
		p.push($(this).attr('name').split("-"));
	});

	window.location = "${h.url_for(action="browser")}?views=" + p;
}
</script>

