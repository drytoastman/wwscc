
<%def name="classlist()">
<table id='classtable' class='res'>
<tbody>
<tr class='titles'>
<th>#</th>
<th>Name</th>
<th>Net</th>
<th>Next</th>
</tr>
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
</%def>



<%def name="champlist()">
%if c.cls.champtrophy:
<table id='champtable' class='res'>
<tbody>
<tr class='titles'>
<th>#</th>
<th>Name</th>
<th></th>
<th>Points</th>
</tr>
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
</%def>


