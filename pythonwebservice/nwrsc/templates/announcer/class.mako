<%inherit file="mobilebase.mako" />

${classlist()}

<%def name="classlist()">

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

</%def>

