<table class='toptimes'>
<tr class='titles'>
<th>#</th>
<th>Name</th>
<th>Index</th>
<th>Time</th>
</tr>
</tr>

%for entry in c.toptimes:
<tr class='${c.e2label(entry)}'> 
<td>${entry['position']}</td>
<td>${entry['name']}</td>
<td>${entry.get('index', '')}</td>
<td>${entry['time']}</td>
</tr>
%endfor

</table>
