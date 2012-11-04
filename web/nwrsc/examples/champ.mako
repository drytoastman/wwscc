<%inherit file="/base.mako" />
<!-- Series info -->
<div id='seriesimage'><img src="../../images/wwscctire.gif" alt='WWSCC' /></div>
<div id='seriestitle'>${c.seriesname}</div>
<hr />
<!-- Results -->

<table class='champ'>
%for code, entrants in sorted(c.results.iteritems()):
<% cls = c.classdata.classlist[code] %>

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
	%for value in [e.points.get(e.id) for e in c.events]:
		<td class='points ${value in e.points.drop and "drop" or ""}'>${h.t3(value)}</td>
	%endfor
	<td class='points'>${h.t3(e.points.total)}</td>
	</tr>
%endfor
%endfor
</table>
