<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type='text/css'>
td { font: 0.7em Tahoma; }
th { text-align: left; }
</style>
</%def>

<table>
<%
	from math import ceil
	count = len(c.beforelist)
	height = int(ceil(count/5.0))
%>
%for ii in range(0, height):
	<tr>
	%for jj in range(ii, count, height):
		<td>${c.beforelist[jj][1].capitalize()}, ${c.beforelist[jj][0].capitalize()}</td>
	%endfor
	</tr>
%endfor
</table>
