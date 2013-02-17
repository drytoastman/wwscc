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

%

<%
codeset = set([v['code'] for v in c.views])
def idfor(v):
	return v['type']+v['code']
def titlefor(v):
	if (v['code'] == 'Any'):
		return v['type'];
	return "%s - %s" % (v['code'], v['type'])
%>
	
<body> 

<div data-role="page" data-theme="b">
	<div data-role="header" data-id="header" data-position="fixed">
		<h1>Browser</h1>
		<div data-role="navbar" id="navbar">
			<ul>
				%for v in c.views:
					<li><a onclick='switchto("${idfor(v)}");'>${titlefor(v)}</a></li>
				%endfor
			</ul>
		</div>
	</div>

	<div data-role="content">
		%for v in c.views:
			<div id="${idfor(v)}" data-type='${v["type"]}' data-code='${v["code"]}' class='viewer' style='display:none;'></div>
		%endfor
	</div>

</div>

</body>
</html>
