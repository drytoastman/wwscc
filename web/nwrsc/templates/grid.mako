<%inherit file="base.mako" />

<style>
table {
	width: 600px;
	border-collapse: collapse;
}
th, td {
	border: 1px solid #777;
}
td {
	padding-left: 8px;
}
</style>

<%def name="entry(e)">
%if e is not None:
%if e.car.sum > 0:
	${e.car.classcode}/${e.car.number} - ${e.driver.firstname} ${e.driver.lastname} (${"%0.3lf"%e.car.sum})
%else:
	${e.car.classcode}/${e.car.number} - ${e.driver.firstname} ${e.driver.lastname}
%endif
%endif
</%def>

<%def name="entries(e1, e2)">
<tr>
<th>${c.index}</th><td>${entry(e1)}</td>
<th>${c.index+1}</th><td>${entry(e2)}</td>
</tr>
<% c.index += 2 %>
</%def>

<%
index1 = 1
index2 = 101
%>

%for groupnum, group in enumerate(c.groups):
<h3>Group ${groupnum+1}</h3>
<table>
<% c.index = index1 %>
%for lft,rht in group[0]:
${entries(lft, rht)}
%endfor
<% index1 = c.index %>
</table>

<h4>Group ${groupnum+1} Dual</h4>
<table>
<% c.index = index2 %>
%for lft,rht in group[1]:
${entries(lft, rht)}
%endfor
<% index2 = c.index %>
</table>

<p style='page-break-before: always'/>

%endfor

