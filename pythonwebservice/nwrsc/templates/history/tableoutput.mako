<%inherit file="/base.mako" />


<table id='historytable'>
<thead>
<tr>
%for name in c.names:
<th>${name}</th>
%endfor
</tr>
</thead>

<tbody>
%for result in c.results:
<tr>
%for name in c.names:
<td>${getattr(result, name)}</td>
%endfor
</tr>
%endfor
</tbody>
</table>

