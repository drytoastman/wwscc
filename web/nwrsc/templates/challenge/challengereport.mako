<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type='text/css'>
tr.winner { background: #EE8; }
td { padding-left: 8px; padding-left: 8px; }
p.winner { text-align:center; font-size: 1.2em; }
span.dial { font-weight: bold; }
</style>
</%def>

<h1>${c.challenge.name}</h1>
<%
for round in sorted(c.rounds.keys(), reverse=True):
	if round <= 98 and round > 1:
		context.write({
				31:"<hr><h2>Bittersweet Thirty-two</h2>",
				15:"<hr><h2>Sweet Sixteen</h2>",
				7: "<hr><h2>Quarter Finals</h2>",	
				3: "<hr><h2>Semi Finals</h2>"}.get(round, ""))
		roundReport(c.rounds[round])
%>
<hr><h2>Third Place</h2>
${roundReport(c.rounds[99])}
<hr><h2>Final</h2>
${roundReport(c.rounds[1])}


<%def name="roundReport(round)">
	<%
		if round.car1id <= 0 or round.car2id <= 0:
			context.write("<h5>No matchup yet</h5>")
			return

		topclass = ''
		botclass = ''
		winstring = ''
		if round.car1leftrun is None and round.car1rightrun is None:
			winstring = 'No runs taken'
		elif round.car1leftrun and round.car1leftrun.status != "OK":
			botclass = 'winner'
			winstring = "%s wins by default" % (round.driver2.firstname)
		elif round.car2rightrun and round.car2rightrun.status != "OK":
			topclass = 'winner'
			winstring = "%s wins by default" % (round.driver1.firstname)
		elif round.car1rightrun and round.car1rightrun.status != "OK":
			botclass = 'winner'
			winstring = "%s wins by default" % (round.driver2.firstname)
		elif round.car2leftrun and round.car2leftrun.status != "OK":
			topclass = 'winner'
			winstring = "%s wins by default" % (round.driver1.firstname)
		elif round.car1result < round.car2result:
			topclass = 'winner'
			winstring = "%s wins by %0.3f" % (round.driver1.firstname, (round.car2result - round.car1result))
		elif round.car2result < round.car1result:
			botclass = 'winner'
			winstring = "%s wins by %0.3f" % (round.driver2.firstname, (round.car1result - round.car2result))
		else:
			result = round.getHalfResult()
			if result > 0:
				winstring = '%s leads by %0.3f' % (round.driver2.firstname, result)
			elif result < 0:
				winstring = '%s leads by %0.3f' % (round.driver1.firstname, result)
			else:
				winstring = 'Tied'
	%>

	<table class='challengeround' border=1>
	<tr><th>Entrant<th><th>Reaction<th>Sixty<th>Time<th>Diff<th>Total<th>NewDial</tr>
	<tr class='${topclass}'>
	${driverCell(round.driver1.firstname, round.car1dial, round.car1.classcode, round.car1.indexcode)}
	<td>L</td>
	${runRow(round.car1dial, round.car1leftrun)}
	${runTotal(round.car1dial, round.car1leftrun, round.car1rightrun, round.car1newdial)}
	</tr>
	<tr class='${topclass}'>
	<td>R</td>
	${runRow(round.car1dial, round.car1rightrun)}
	</tr>
	<tr class='${botclass}'>
	${driverCell(round.driver2.firstname, round.car2dial, round.car2.classcode, round.car2.indexcode)}
	<td>L</td>
	${runRow(round.car2dial, round.car2leftrun)}
	${runTotal(round.car2dial, round.car2leftrun, round.car2rightrun, round.car2newdial)}
	</tr>
	<tr class='${botclass}'>
	<td>R</td>
	${runRow(round.car2dial, round.car2rightrun)}
	</tr>
	</table>
	<P class='winner'>${winstring}</P>
</%def>

<%def name="driverCell(name, dial, classname, index)">
	<td rowspan=2><span class='name'>${name}</span><span class='dial'>${h.t3(dial)}</span><br/>
	<span class='class'>${classname}</span><span class='index'>(${index})</span>
</%def>

<%def name="runRow(dial, run)">
	%if run is None:
		<td></td><td></td><td></td><td></td>
	%else:
		<td>${h.t3(run.reaction)}</td>
		<td>${h.t3(run.sixty)}</td>
		<td>${h.t3(run.raw)} (+${run.cones})</td>
		%if run.status != "OK":
			<td>${run.status}</td>
		%else:
			<td>${h.t3(run.net, dial)}</td>
		%endif
	%endif
</%def>

<%def name="runTotal(dial, left, right, newdial)">
	<td rowspan=2>
	%if left is None or right is None:
		</td>
	%elif left.status != "OK" and left.status != "":
		${left.status}</td>
	%elif right.status != "OK" and right.status != "":
		${right.status}</td>
	%elif left.net == 0.0 or right.net == 0.0:
		</td>
	%else:
		${"%+0.3f"%(left.net + right.net - (2*dial))}</td>
	%endif

	<td rowspan=2>
	%if newdial != dial and newdial != 0.0:
		${h.t3(newdial)}
	%endif
	</td>
</%def>
