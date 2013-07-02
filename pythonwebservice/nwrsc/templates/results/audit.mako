<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type='text/css'>
table.auditreport td, table.auditreport th { border: 1px solid #AAA; padding: 5px; }
table.auditreport .bold { font-weight: bold; }
</style>
</%def>

<table class='auditreport'>
<thead><tr>
<th>First</th>
<th>Last</th>
<th>#</th>
<th>Cls</th>
%for ii in range(c.event.runs):
	<th>Run${ii+1}</th>
%endfor
</tr>
</thead>
<tbody>

%for entrant in c.entrants:
	<tr>
	<td class='${c.order == "firstname" and "bold" or ""}'>${entrant.firstname[:8]}</td>
	<td class='${c.order == "lastname" and "bold" or ""}'>${entrant.lastname[:8]}</td>
	<td>${entrant.number}</td>
	<td>${entrant.classcode} ${h.ixstr(entrant)}</td>

	%for run in entrant.runs:
		<td>
		%if run is not None:
			%if run.status != "OK":
				${run.status}
			%else:
				${"%0.3f " % run.raw}
				${"(%d,%d)" % (run.cones, run.gates)}
			%endif
		%endif
		</td>
	%endfor
	
	</tr>
%endfor

</tbody>
</table>
