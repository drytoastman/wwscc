<%inherit file="/base.mako" />

${resulttables()}

<%def name="resulttables()">
<table class='classresults'>
<%
c.colcount = 6 + c.event.runs
c.rowspan = "rowspan='%s'" % (c.event.courses)


for code in sorted(c.results.keys()):
	entrants = c.results[code]
	c.curclass = c.classdata.classlist[code]
	headerrow()
	for e in entrants:
		entrantrow(e)
		for runs in e.runs[1:]:
			context.write("<tr>\n")
			for run in runs:
				runcell(run)
			context.write("</tr>\n")
%>
</table>
</%def>


<%def name="headerrow()">
<tr>
	<th class='classhead' colspan='${c.colcount}'>
	<a name='${c.curclass.code}'>${c.curclass.code}</a> - ${c.curclass.descrip}
	</th>
</tr>
<tr>
<th class='pos'>#</th>
<th class='trophy'>T</th>
<th class='name'>Entrant</th>
%for ii in range(1, int(c.event.runs)+1):
	<th class='run'>Run ${ii}</th>
%endfor
<th></th>  <%doc> Extra col to fix firefox border bug </%doc>
<th class='total'>Total</th>
<th class='points'>Points</th>
</tr>
</%def>


<%def name="entrantrow(entrant)">
<tr>
	<td class='pos' ${c.rowspan}>${entrant.position}</td>
	<td class='trophy' ${c.rowspan}>${entrant.trophy and 'T' or ''}</td>
	<td class='name' align='center' ${c.rowspan}>#${entrant.number} - ${entrant.firstname} ${entrant.lastname}<br/>
		${entrant.make} ${entrant.model} ${entrant.color} ${entrant.indexstr and "(%s)"%entrant.indexstr or ""}</td>
	%for run in entrant.runs[0]:
		${runcell(run)}
	%endfor
	<td ${c.rowspan}></td> <%doc> Extra col to fix firefox border bug </%doc>
	<td class='total' ${c.rowspan}>${h.t3(entrant.sum)}</td>
	<td class='points' ${c.rowspan}>${h.t3(entrant.points)}</td>
	<%doc> Can reference entrant.pospoints or entrant.diffpoints to bypass series setting for points type </%doc>
</tr>
</%def>


<%def name="runcell(run)" filter="trim">
%if run is None:
<td class='run'>

%else:
<td class='run ${run.norder==1 and 'bestnet' or run.rorder==1 and 'bestraw' or ''}'>
%if run.status == "OK":
	<span class='net'>${h.t3(run.net)}</span>
%else:
	<span class='net'>${run.status}</span>
%endif
<span class='raw'>${h.t3(run.raw)} (${run.cones},${run.gates})</span>
<span class='reaction'>${h.t3(run.reaction)}/${h.t3(run.sixty)}</span>

%endif
</td>
</%def>

