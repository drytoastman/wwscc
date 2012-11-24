<%inherit file="/base.mako" />
<%namespace file="db:classresult.mako" import="resulttables"/>
<%namespace file="db:toptimes.mako" import="toptimestable"/>
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

${toptimestable([c.toptimes.getList(allruns=False, raw=True, course=0, settitle="Top Times")])}

<!--#include virtual="/wresfooter.html" -->
