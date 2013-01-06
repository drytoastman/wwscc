<!DOCTYPE html> 
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Live Results Selection</title> 

	<style type='text/css'>

	table { 
		border-collapse: collapse;
	}

	td {
		text-align: center
		border-bottom: 1px solid gray; 
		padding: 2px;
	}

	.button {
		-moz-box-shadow:inset 0px 1px 0px 0px #bbdaf7;
		-webkit-box-shadow:inset 0px 1px 0px 0px #bbdaf7;
		box-shadow:inset 0px 1px 0px 0px #bbdaf7;
		background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #79bbff), color-stop(1, #378de5) );
		background:-moz-linear-gradient( center top, #79bbff 5%, #378de5 100% );
		filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#79bbff', endColorstr='#378de5');
		background-color:#79bbff;
		-moz-border-radius:6px;
		-webkit-border-radius:6px;
		border-radius:6px;
		border:1px solid #84bbf3;
		display:inline-block;
		color:#ffffff;
		font-family:arial;
		font-size:12px;
		font-weight:bold;
		padding:6px 24px;
		text-decoration:none;
		text-shadow:1px 1px 0px #528ecc;
	}

	.button:hover {
		background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #378de5), color-stop(1, #79bbff) );
		background:-moz-linear-gradient( center top, #378de5 5%, #79bbff 100% );
		filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#378de5', endColorstr='#79bbff');
		background-color:#378de5;
	}

	.button:active {
		position:relative;
		top:1px;
	}

	</style>

</head> 

<%
def makeurl(code, usepax, useraw):
	views = [code, "Event", code, "Champ"]
	if usepax: views.extend(["Any", "PAX"])
	if useraw: views.extend(["Any", "Raw"])
	return h.url_for(action='browser') + '?views=' + ','.join(views)
%>

<body> 

	<h1>View Selection</h1>
	<table>
		%for cls in sorted([x.code for x in c.classes]):
		<tr>
			<td><label>${cls}</label></td>
			<td><a class='button' href='${makeurl(cls, False, False)}'>Basic</a></td>
			<td><a class='button' href='${makeurl(cls, True, False)}'>+PAX</a></td>
			<td><a class='button' href='${makeurl(cls, True, True)}'>+Raw</a></td>
		</tr>
		%endfor
	</table>

</body>
</html>

