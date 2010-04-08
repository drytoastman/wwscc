<%inherit file="/base.mako" />

<%def name="toptimes(title, times)">
<table class='toptimes'>
<tr><th class='classhead' colspan='4'>${title}</th></tr>
<tr><th>#</th><th>Name</th><th>Class</th><th>Time</th></tr>

%for ii, entrant in enumerate(times):
<tr>
<td>${ii+1}</td>
<td>${entrant.firstname} ${entrant.lastname}</td>
<td>${entrant.classcode}</td>
<td>${"%0.3f"%entrant.toptime}</td>
</tr>
%endfor

</table>
</%def>

<%def name="topindextimes(title, times)">
<table class='toptimes'>
<tr><th class='classhead' colspan='5'>${title}</th></tr>
<tr><th>#</th><th>Name</th><th colspan='2'>Index</th><th>Time</th></tr>

%for ii, entrant in enumerate(times):
<tr>
<td>${ii+1}</td>
<td>${entrant.firstname} ${entrant.lastname}</td>
<td>${"%0.3f"%entrant.indexval}</td>
<td>${entrant.indexstr}</td>
<td>${"%0.3f"%entrant.toptime}</td>
</tr>
%endfor

</table>
</%def>

%if c.topindextimes is not None:
<center>
<table>
%for ii, times in enumerate(c.topindextimes):
${self.topindextimes(not ii and 'Top Index Times' or 'Top Index (Course %d)' % ii, times)} 
%endfor
</table>
</center>
%endif

%if c.toptimes is not None:
<center>
<table>
%for ii, times in enumerate(c.toptimes):
${self.toptimes(not ii and 'Top Times' or 'Top Times (Course %d)' % ii, times)} 
%endfor
</table>
</center>
%endif
