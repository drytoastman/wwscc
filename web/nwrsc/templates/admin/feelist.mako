<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type='text/css'>
td { font: 0.7em Tahoma; }
th { text-align: left; }
</style>
</%def>

<h2>${c.header}</h2>
<table>
<%
	from math import ceil
	count = len(c.feelist)
	height = int(ceil(count/5.0))
%>
%for ii in range(0, height):
	<tr>
	%for jj in range(ii, count, height):
		<td>${c.feelist[jj][1].capitalize()}, ${c.feelist[jj][0].capitalize()}</td>
	%endfor
	</tr>
%endfor
</table>
