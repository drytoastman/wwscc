<%inherit file="mobilebase.mako" />

${runslist()}

<%def name="runslist()">

<table id='resultstable' class='res'>
<%
import operator
ecolumns = 5 + c.event.getSegmentCount()
%>
<tbody>
<tr class='header'><th colspan='${ecolumns}'>${c.driver.firstname} ${c.driver.lastname}</th></tr>
<tr class='titles'><th>#</th><th>Raw</th>
%for jj in range(1, c.event.getSegmentCount()+1):
<th>Seg ${jj}</th>
%endfor
<th>C</th><th>G</th><th>Net</th></tr>

%for run in c.runs:

<% sega = run.validSegments(c.event.getSegments()) %>
<tr class='${c.e2label(run)}'>
<td>${run.run}</td>
<td>${h.t3(run.raw)}
	%if hasattr(run, 'rawdiff'):
		(${h.t3(run.rawdiff)})
	%endif
</td>
%for jj in range(c.event.getSegmentCount()):
<td>${h.t3(sega[jj])}</td>
%endfor
<td>${run.cones}</td>
<td>${run.gates}</td>
<td>${h.t3(run.net)}
	%if hasattr(run, 'netdiff'):
		(${h.t3(run.netdiff, sign=True)})
	%endif
</td>
</tr>


%endfor

</tbody>
</table>
</%def>
