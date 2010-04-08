<%inherit file="/base.mako" />
<!-- Series info -->
<div id='seriesimage'><img src="../../images/wwscctire.gif" alt='WWSCC' /></div>
<div id='seriestitle'>${c.seriesname}</div>
<hr />
<!-- Results -->

<table class='champ'>
%for code, entrants in sorted(c.results.iteritems()):
<%
import operator
cls = c.classdata.classlist[code]
if not cls.champtrophy: # No champ trophies for this class
	continue
if len(entrants) <= 0:
	continue
entrants.sort(key=operator.attrgetter('points'))
%>

<tr><th class='classhead' colspan='${len(c.events)+4}'>${cls.code} - ${cls.descrip}</th></tr>
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
	%for event in sorted(c.events, key=lambda obj: obj.date):
		<% 
			value = e.points.get(event.id)
			if value in e.points.drop:
				tdclass = 'drop'
			else:
				tdclass = ''
		%>
		%if value is None:
			<td class='points drop'></td>
		%else:
			<td class='points ${tdclass}'>${"%0.3f"%value}</td>
		%endif
	%endfor
	<td class='points'>${"%0.3f"%e.points.total}</td>
	</tr>
%endfor
%endfor
</table>

