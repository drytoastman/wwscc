<table class='toptimes'>
<tr class='titles'>
<th>#</th>
%for col in c.toptimes.cols:
<th>${col}</th>
%endfor
</tr>

%for ii, (carid, listrow) in enumerate(zip(c.toptimes.carids, c.toptimes.rows)):
<tr class='${(carid==c.highlight) and 'highlight' or ''}'>
<td>${ii+1}</td>
%for item in listrow:
<td>${item}</td>
%endfor
</tr>
%endfor

</table>
