
%if c.cls.champtrophy:
<table id='champtable' class='res'>
<tbody>
<tr class='header'><th colspan='4'>Champ - ${c.cls.code}</th></tr>
<tr class='titles'><th>#</th><th>Name</th><th></th><th>Points</th></tr>
%for ii, e in enumerate(sorted(c.champresults, key=h.attrgetter('points'))):
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

