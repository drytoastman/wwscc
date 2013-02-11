<%inherit file="/base.mako" />
<!-- Series info -->
<div id='seriesimage'><img src="../../images/smlnwr.jpg"><img src="../../images/soloev.gif"></div>
<div id='seriestitle'>${c.seriesname}</div>
<hr />
<!-- Results -->

<table class='champ'>
%for code, entrants in sorted(c.results.iteritems()):
<%
cls = c.classdata.classlist[code]
totalentrants = 0
for e in entrants:
	totalentrants += e.events
avgentrants = float(totalentrants)/len(c.events)
%>

<tr><th class='classhead' colspan='${len(c.events)+4}'>${cls.code} - ${cls.descrip} -
			Number of Entries: ${totalentrants} - Average Per Event: ${"%.2lf"%avgentrants}</th></tr>
<tr>
<th>#</th>
<th>Name</th>
<th>Attended</th>
%for jj, event in enumerate(c.events): 
<th>Event ${jj+1}</th>
%endfor
<th>Total</th>
</tr>
		
%for ii, e in enumerate(entrants):
	<tr>
	<td>${ii+1}</td>
	<td class='name'>${e.firstname} ${e.lastname}</td>
	<td class='attend'>${e.events}</td>
	%for value in [e.points.get(event.id) for event in c.events]:
		<td class='points ${value in e.points.drop and "drop" or ""}'>${h.t3(value)}</td>
	%endfor
	<td class='points'>${h.t3(e.points.total)}</td>
	</tr>
%endfor
%endfor
</table>
