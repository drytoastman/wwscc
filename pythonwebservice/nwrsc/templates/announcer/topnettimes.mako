<table class='toptimes res'>
<tr class='titles'>
<th>#</th>
<th>Name</th>
<th>Class</th>
<th colspan=2>Index</th>
<th>Time</th>
</tr>
</tr>

%for entry in c.toptimes:
<tr class='${c.e2label(entry)}'> 
<td>${entry.position}</td>
<td>${entry.name}</td>
<td>${entry.classcode}</td>
<td>${entry.indexcode}</td>
<td>${entry.indexvalue}</td>
<td>${entry.toptime}</td>
</tr>
%endfor

</table>
