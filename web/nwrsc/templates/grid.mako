<%inherit file="base.mako" />

<style>
table { width: 600px; }
</style>
<%def name="entry(e)">
%if e is not None:
${e.car.classcode}/${e.car.number} - ${e.driver.firstname} ${e.driver.lastname}
%endif
</%def>

<%def name="entries(code, myiter)">
%for e1, e2 in zip(myiter, myiter):
<tr><td>${c.index}</td> <td>${entry(e1)}</td> <td>${c.index+1}</td> <td>${entry(e2)}</td></tr>
<% c.index += 2 %>
%endfor
</%def>


%for group in c.groups[1:]:
<h3>Group ${group.groupnum}</h3>

<table border=1>
<% c.index = 1 %>
%for cls in group.classes:
%if len(cls.first) > 0:
${entries(cls.code, iter(cls.first + [None]))}
%endif
%endfor
</table>

<h4>Group ${group.groupnum} Dual</h4>
<table border=1>
<% c.index = 100 %>
%for cls in group.classes:
%if len(cls.second) > 0:
${entries(cls.code, iter(cls.second + [None]))}
%endif
%endfor
</table>

%endfor

