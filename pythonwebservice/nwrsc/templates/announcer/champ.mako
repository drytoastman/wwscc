<%inherit file="mobilebase.mako" />

${champlist()}

<%def name="champlist()">
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

</%def>

