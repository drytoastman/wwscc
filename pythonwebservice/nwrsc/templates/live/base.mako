<!DOCTYPE html> 
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1"> 
	<title>Live Results</title> 
	<link rel="stylesheet" href="/css/live.css" />
	<script src="/js/live.js"></script>
<script type='text/javascript'>
<%
try:
	context.write('$.nwr.urlbase = "%s";' % h.url_for(action=''));
except:
	pass
%>
</script>
</head> 

<body> 

${next.body()}

</body>
</html>
