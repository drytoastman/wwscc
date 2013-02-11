<%inherit file="/base.mako" />
<%namespace file="db:classresult.mako" import="resulttables"/>
<%namespace file="db:toptimes.mako" import="toptimestable"/>
<!-- Series info -->
<div id='seriesimage'>
<img src="../../images/smlnwr.jpg">
<img src="../../images/soloev.gif">
</div>
<center>
<B><FONT face="Tahoma" color="#008000" size="+3">Northwest Region SCCA</FONT></B>
</center>

<div id='seriestitle'>${c.seriesname}</div>

<!-- Event info -->
<div id='eventtitle'>${c.event.date} - ${c.event.name}</div>
<div id='hosttitle'>Hosted At: <span class='host'>${c.event.location}</span></div>
<div id='entrantcount'>(${c.entrantcount} Entrants)</div>

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
${toptimestable([c.toptimes.getList(allruns=False, raw=False, course=0), c.toptimes.getList(allruns=False, raw=True, course=0)])}


<!--#include virtual="/wresfooter.html" -->
