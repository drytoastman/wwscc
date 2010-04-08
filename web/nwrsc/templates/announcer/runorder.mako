<table class='runorder'>
<tbody>
<tr class='header'><th colspan='6'>Next To Finish</th></tr>
<tr class='titles'><th>Name</th><th>Car</th><th>Class</th><th>Best</th><th>Pos</th><th>Need</th></tr>
%for e,r in c.order:
<tr>
<td>${e.firstname} ${e.lastname}</td>
<td>${e.year} ${e.model} ${e.color}</td>
<td>${e.classcode}</td>
%if r is not None:
<td>${"%0.3f (%d,%d)" % (r.raw, r.cones, r.gates)}</td>
<td>${r.position}</td>
<td>${"%0.3f"%r.diff}</td>
%else:
<td></td><td></td><td></td>
%endif
</tr>
%endfor
</tbody>
</table>
