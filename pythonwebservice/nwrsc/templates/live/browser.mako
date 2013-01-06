<!DOCTYPE html> 
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1"> 
	<title>Live Results</title> 
	<link rel="stylesheet" href="/css/jquery.mobile-1.2.0.min.css" />
	<script src="/js/jquery-1.8.3.min.js"></script>
	<script src="/js/jquery.mobile-1.2.0.min.js"></script>
</head> 

<%
codeset = set([v['code'] for v in c.views])
def idof(v): return v['type']+v['code']
def titlefor(v):
	if (v['code'] == 'Any'):
		return v['type'];
	return "%s - %s" % (v['code'], v['type'])
%>
	
<body> 

<div data-role="page" data-theme="b">
	<div data-role="header" data-id="header" data-position="fixed">
		<h1>Browser</h1>
		<div data-role="navbar">
			<ul>
				%for v in c.views:
					<li><a onclick='switchto("${idof(v)}");'>${titlefor(v)}</a></li>
				%endfor
			</ul>
		</div>
	</div>

	<div data-role="content">
		%for v in c.views:
			<div id="${idof(v)}" style='display:none;'></div>
		%endfor
	</div>

</div>

</body>
</html>

<style type='text/css'>
table
{
	background: white;
	border: 1px solid gray;
	border-collapse: collapse;
	width: 100%;
}

table td, table th
{ 
	color: black; 
	border: 1px solid gray;
	text-shadow: none;  
}

tr.highlight td {
	font-weight: bold;
	background: #DDD; 
}

tr.improvedon td {
	font-weight: bold;
	background: #EEF; 
	color: #99E;
}

tr.couldhave td {
	font-weight: bold;
	background: #EDD; 
	color: #E77;
}

</style>

<script type='text/javascript'>

var baseurl = "${h.url_for(action='')}";
var current = "ignoremenotpresent";
var views = Array();
%for code in codeset:
views["${code}"] = Object();
views["${code}"].updated = 0;
views["${code}"].pages = Array();
%endfor
%for v in c.views:
views["${v['code']}"].pages.push(function(carid) { $("#${idof(v)}").load("${h.url_for(action=v['type'])}/"+carid); });
%endfor

function switchto(newid)
{
	$('#'+current).css('display', 'none');
	$('#'+newid).css('display', 'block');
	current = newid;
}

function processLast(json)
{
	for (index in json)
	{
		obj = json[index];
		match = views[obj.classcode]
		if (obj.updated >  match.updated)
		{
			match.updated = obj.updated;
			for (index1 in match.pages)
			{
				match.pages[index1](obj.carid);
			}
		}
	}
}

function updateCheck()
{
	$.getJSON('${h.url_for(action='last')}', { classcodes: "${','.join(codeset)|n}" }, processLast);
}

$(document).ready(function(){
	updateCheck();
	setInterval('updateCheck()', 3000);
});


</script>

