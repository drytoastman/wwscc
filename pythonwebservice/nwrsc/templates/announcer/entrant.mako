<!-- runs -->
<table id='resultstable' class='res'>
<tbody>
<tr class='header'><th colspan='${5 + c.event.getSegmentCount()}'>${c.driver.firstname} ${c.driver.lastname}</th></tr>
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


<!-- end runs -->

<table style='width:100%; margin-top:5px;'><tr><td width=50%>

<!-- class list -->

<table id='classtable' class='res'>
<tbody>
<tr class='header'><th colspan='4'>Event - ${c.cls.code}</th></tr>
<tr class='titles'><th>#</th><th>Name</th><th>Net</th><th>Next</th></tr>
%for e in c.results:
<tr class='${c.e2label(e)}'>
<td>${e['position']}</td>
<td>${e['firstname']} ${e['lastname']}</td>
<td>${e['sum']}</td>
<td>${e.get('diff', '')}</td>
</tr>
%endfor
</tbody>
</table>

<!-- end class list -->

</td><td width=50%>

<!-- champlist -->

%if c.cls.champtrophy:
<table id='champtable' class='res'>
<tbody>
<tr class='header'><th colspan='4'>Champ - ${c.cls.code}</th></tr>
<tr class='titles'><th>#</th><th>Name</th><th></th><th>Points</th></tr>
%for e in c.champ:
<tr class='${c.e2label(e)}'>
<td>${e['position']}</td>
<td>${e['firstname']} ${e['lastname']}</td>
<td>${e['events']}</td>
<td>${e['points']}</td>
</tr>
%endfor
</tbody>
</table>
%endif

<!-- end champ list -->

</td></tr></table>


