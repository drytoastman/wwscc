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
views = [
		 {'id':'one', 'code':'OPAX', 'type':'Event'},
		 {'id':'two', 'code':'OPAX', 'type':'Champ'}, 
		 {'id':'three', 'code':'*',  'type':'PAX'}
		]
codeset = set([v['code'] for v in views])
%>
	
<body> 

%for ii, view in enumerate(views):
<div data-role="page" data-theme="a" id="${view['id']}">
	<div data-role="header" data-id="header${ii}" data-position="fixed">
		%if ii > 0:
		<a href="#${views[ii-1]['id']}" data-icon="arrow-l" data-role="button" >Prev</a>
		%endif
		<h1>${view['code']} - ${view['type']}</h1>
		%if ii < len(views)-1:
		<a href="#${views[ii+1]['id']}" data-icon="arrow-r" data-role="button" class="ui-btn-right">Next</a>
		%endif
	</div>
	<div data-role="content" id="content-${view['id']}">
	</div>
</div>

%endfor

</body>
</html>


<script>

var baseurl = "${h.url_for(action='')}";
var views = Array();
%for code in codeset:
views["${code}"] = Object();
views["${code}"].updated = 0;
views["${code}"].pages = Array();
%endfor
%for v in views:
views["${v['code']}"].pages.push(function(carid) {
	$("#content-${v['id']|n}").load("${h.url_for(action=v['type'])|n}/"+carid);
});
%endfor

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
	$.getJSON('${h.url_for(action='last')}', { class: "${','.join(codeset)}" }, processLast);
}

$(document).on('pageinit', function() {
	updateCheck();
	setInterval('updateCheck()', 3000);
});


</script>

