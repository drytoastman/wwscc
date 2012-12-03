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

%for ii, run in enumerate([c.runs.get(index,None) for index in range(1, c.event.runs+1)]):
%if run is not None:

<%
sega = run.validSegments(c.event.getSegments())
trclass = ""
if run.norder == 1: trclass = 'highlight'
if run == c.couldhaverun: trclass = 'couldhave'
%>
<tr class='${trclass}'>
<td>${ii+1}</td>
<td>${h.t3(run.raw)}
	%if run.rdiff:
		(${h.t3(run.rdiff)})
	%endif
</td>
%for jj in range(c.event.getSegmentCount()):
<td>${h.t3(sega[jj])}</td>
%endfor
<td>${run.cones}</td>
<td>${run.gates}</td>
<td>${h.t3(run.net)}
	%if run.ndiff:
		(${h.t3(run.ndiff, sign=True)})
	%endif
</td>
</tr>


%else:


<tr>
<td></td>
%for jj in range(c.event.getSegmentCount()):
<td></td>
%endfor
<td></td>
<td></td>
<td></td>
<td></td>
</tr>

%endif
%endfor

</tbody>
</table>
</%def>
