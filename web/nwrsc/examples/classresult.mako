<%inherit file="/base.mako" />

${resulttables()}

<%def name="resulttables()">
<table class='classresults'>
<%
if c.event.courses > 1:
	c.colcount = 8 + c.event.runs
	c.rowspan = "rowspan='%s'" % (c.event.courses)
else:
	c.colcount = 7 + c.event.runs
	c.rowspan = ""


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
	<th class='pos'></th>
	<th class='trophy'></th>
	<th class='name'>Name</th>
	<th class='carnum'>#</th>
	<th class='caryear'>Year</th>
	<th class='cardesc'>Make/Model</th>
	%for ii in range(1, int(c.event.runs)+1):
	<th class='run'>Run${ii}</th>
	%endfor
	%if c.event.courses > 1:
	<th class='total'>Total</th>
	%endif
	<th class='points'>Points</th>
</tr>
</%def>


<%def name="entrantrow(entrant)">
<tr>
	<td class='pos' ${c.rowspan}>${entrant.position}</td>
	<td class='trophy' ${c.rowspan}>${entrant.trophy and 'T' or ''}</td>
	<td class='name' ${c.rowspan}>${entrant.firstname+" "+entrant.lastname}</td>
	<td class='carnum' ${c.rowspan}>${entrant.number}</td>
	<td class='caryear' ${c.rowspan}>${entrant.year}</td>
	<td class='cardesc' ${c.rowspan}>${entrant.make} ${entrant.model} ${entrant.color} ${entrant.indexstr and "(%s)"%entrant.indexstr or ""}</td>
	%for run in entrant.runs[0]:
	${runcell(run)}
	%endfor
	%if c.event.courses > 1:
	<td class='total' ${c.rowspan}>${h.t3(entrant.sum)}</td>
	%endif
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
		<span class='net'>${h.t3(run.net)} (${run.cones},${run.gates})</span>
		%if c.curclass.carindexed or c.curclass.classindex:
			<span class='raw'>[${h.t3(run.raw)}]</span>
		%endif
	%else:
		<span class='net'>${run.status}</span>
	%endif
	%if c.event.getSegmentCount() > 0:
		<span class='reaction'>${'/'.join(map(h.t3, run.validSegments(c.event.getSegments())))}</span>
	%endif
%endif
</td>
</%def>
