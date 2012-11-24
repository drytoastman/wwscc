<%inherit file="/base.mako" />
<%def name="extrahead()">
<style>
td { font: 0.7em Tahoma; }
th { text-align: left; }
</style>
</%def>

<h2>Used Number List</h2>
<table>
%for code in sorted(c.numbers):
	<tr><th colspan='2'>${code}</th></tr>
	<%
		from math import ceil
		count = len(c.numbers[code])
		height = int(ceil(count/4.0))
		order = sorted(c.numbers[code].keys())
	%>
	%for ii in range(0, height):
		<tr>
		%for jj in range(ii, count, height):
			<td>${order[jj]}</td><td>${','.join(c.numbers[code][order[jj]])}</td>
		%endfor
		</tr>
	%endfor
%endfor
</table>
