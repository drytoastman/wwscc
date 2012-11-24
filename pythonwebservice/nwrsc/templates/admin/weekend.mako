<%inherit file="/base.mako" />
<style>
table {
	border-collapse: collapse;
}
th, td { 
	border: 1px solid #AAA;
	text-align: center;
	padding: 2px;
}
</style>

<h2>Weekend Report</h2>
<table>
<tr>
<th>Dates (Wed-Tues)</th>
<th>Events</th>
<th>Unique Drivers</th>
<th>Membership</th>
</tr>

%for start in sorted(c.weeks):
<%
	import datetime
	report = c.weeks[start]
	end = start + datetime.timedelta(+6)
%>
<tr>
<td>
<div>${start}</div>
<div>${end}</div>
</td>

<td>
%for e in report.events:
<div>${e.name}</div>
%endfor
</td>


<td>
${len(report.drivers)}
</td>


<td>
<div>${', '.join(report.membership)}</div>
%if len(report.invalid) > 0:
<div>${len(report.invalid)} bad numbers</div>
<div>${', '.join(report.invalid)}</div>
%endif
</td>

</tr>
%endfor

</table>
