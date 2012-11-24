<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type='text/css'>
td { font: 0.7em Tahoma; }
th { text-align: left; }
</style>
</%def>

<h2>New Entrants Per Event/Fees Collected</h2>
<table>
<% from math import ceil %>
%for f in c.feelists:
	<%
		feelist = f.during
		count = len(feelist)
		height = int(ceil(count/5.0))
	%>
	<tr><th colspan='5'>New Entrants for ${f.name} (${count})</th></tr>
	%for ii in range(0, height):
		<tr>
		%for jj in range(ii, count, height):
			<td>${feelist[jj][1].capitalize()}, ${feelist[jj][0].capitalize()}</td>
		%endfor
		</tr>
	%endfor
%endfor
</table>
