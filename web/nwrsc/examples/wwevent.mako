<%inherit file="/base.mako" />
<%namespace file="db:classresult.mako" import="resulttables"/>
<%namespace file="db:toptimes.mako" import="toptimes, topindextimes"/>
<!-- Series info -->
<div id='seriesimage'><img src="../../images/wwscctire.gif" alt='WWSCC' /></div>
<div id='seriestitle'>${c.seriesname}</div>

<!-- Event info -->
<div id='eventtitle'>${c.event.name} - ${c.event.date}</div>
<div id='hosttitle'>Hosted By: <span class='host'>${c.event.host}</span></div>
<div id='entrantcount'>(${c.entrantcount} Entrants)</div>
<div class='info'>For Indexed Classes Times in Brackets [] Is Raw Time</div>

<hr />
<!-- Results -->

<div id='classlinks'>
%for cls in c.active:
<a href='#${cls.code}'>${cls.code}</a>
%endfor
</div>

${resulttables()}

<br/>
<br/>

<center>
<table>
%for ii, times in enumerate(c.toptimes):
${toptimes(not ii and 'Top Times' or 'Top Times (Course %d)' % ii, times)} 
%endfor
</table>
</center>

<!--#include virtual="/wresfooter.html" -->
