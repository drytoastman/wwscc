<%inherit file="base.mako" />
<h3>Select series:</h3>

<style type='text/css'>
ul.selectorlist { list-style: none;  display:block; float: left; height: 220px; }
</style>

<%
	import operator
	import re
	lists = {}
	default = []
	for db in c.dblist:
		try:
			year = re.search('\d{4}', db.name).group(0)
			if year not in lists: lists[year] = list()
			lists[year].append(db)
		except:
			default.append(db)
%>
	
%for year in sorted(lists, reverse=True):
<ul class='selectorlist'>
	<h4>${year}</h4>
	%for db in sorted(lists[year], key=operator.attrgetter('name')):
		<li><a href='${h.url_for(database=db.name)}'>${db.name}</a></li>
	%endfor
</ul>
%endfor

<ul class='selectorlist'>
<h4>Other</h4>
%for db in default:
	<li><a href='${h.url_for(database=db.name)}'>${db.name}</a></li>
%endfor
</ul>

<br style='clear:all;'/>

