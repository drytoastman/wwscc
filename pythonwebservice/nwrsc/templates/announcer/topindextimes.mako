<table class='toptimes'>
<tr class='titles'><th>#</th><th>Name</th><th colspan='2'>Index</th><th>Time</th></tr>
%for ii, entrant in enumerate(c.toptimes):
<tr class='${(entrant.carid==c.highlight) and 'highlight' or ''}'>
<td>${ii+1}</td>
<td>${entrant.firstname} ${entrant.lastname}</td>
<td>${"%0.3f"%entrant.indexval}</td>
<td>${entrant.indexstr}</td>
<td>${"%0.3f"%entrant.toptime}</td>
</tr>
%endfor

</table>

