<%
import operator
ecolumns = 5 + c.event.getSegmentCount()
%>

<table id='resultstable' class='res'>
<tbody>
<tr class='header'><th colspan='${ecolumns}'>${c.driver.firstname} ${c.driver.lastname}</th></tr>
<tr class='titles'><th>#</th><th>Raw</th>
%for ii in range(1, c.event.getSegmentCount()+1):
<th>Seg ${ii}</th>
%endfor
<th>C</th><th>G</th><th>Net</th></tr>
%for ii in range(1, c.event.runs+1):
<%
run = c.runs.get(ii, None)
if run is not None:
	sega = run.validSegments(c.event.getSegments())
%>
%if run is not None:
<tr class='${(run.norder==1) and 'highlight' or ''}'>
<td>${ii}</td>
<td>${h.t3(run.raw)}</td>
%for ii in range(c.event.getSegmentCount()):
<td>${h.t3(sega[ii])}</td>
%endfor
<td>${run.cones}</td>
<td>${run.gates}</td>
<td>${h.t3(run.net)}</td>
</tr>
%else:
<tr>
<td></td>
%for ii in range(c.event.getSegmentCount()):
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

<table style='width:100%; margin-top:5px;'><tr><td width=50%>

<table id='classtable' class='res'><tbody>
<tr class='header'><th colspan='4'>Event - ${c.cls.code}</th></tr>
<tr class='titles'><th>#</th><th>Name</th><th>Net</th><th>Need</th></tr>
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
</tbody></table>

</td><td width=50%>

%if c.cls.champtrophy:
<table id='champtable' class='res'>
<tbody>
<tr class='header'><th colspan='4'>Champ - ${c.cls.code}</th></tr>
<tr class='titles'><th>#</th><th>Name</th><th></th><th>Points</th></tr>
%for ii, e in enumerate(sorted(c.champresults, key=operator.attrgetter('points'))):
<tr class='${(e.carid==c.highlight) and 'highlight' or ''}'>
<td>${ii+1}</td>
<td>${e.firstname} ${e.lastname}</td>
<td>${e.events}</td>
<td>${h.t3(e.points.total)}</td>
</tr>
%endfor
</tbody>
</table>
%endif

</td></tr></table>




