<!DOCTYPE html> 
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Live Results Selection</title> 
	<link rel="stylesheet" href="/css/live.css" />
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
	<table class='selection'>
		%for cls in sorted([x.code for x in c.classes]):
		<tr>
			<td><label>${cls}</label></td>
			<td><a class='punch' href='${makeurl(cls, False, False)}'>Basic</a></td>
			<td><a class='punch' href='${makeurl(cls, True, False)}'>+PAX</a></td>
			<td><a class='punch' href='${makeurl(cls, True, True)}'>+Raw</a></td>
		</tr>
		%endfor
	</table>

</body>
</html>

