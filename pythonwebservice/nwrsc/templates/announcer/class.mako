<%inherit file="mobilebase.mako" />

${classlist()}

<%def name="classlist()">

<table id='classtable' class='res'>
<tbody>
<tr class='header'><th colspan='4'>Event - ${c.cls.code}</th></tr>
<tr class='titles'><th>#</th><th>Name</th><th>Net</th><th>Next</th></tr>
%for e in c.results:
<%
	trclass = ""
	if e.carid == c.highlight: trclass = 'highlight'
	if e == c.improvedon: trclass = 'improvedon'
	if e == c.couldhave: trclass = 'couldhave'
%>
<tr class='${trclass}'>
<td>${e.position}</td>
<td>${e.firstname} ${e.lastname}</td>
<td>${h.t3(e.sum)}</td>
<td>${h.t3(e.diff)}</td>
</tr>
%endfor
</tbody>
</table>

</%def>

