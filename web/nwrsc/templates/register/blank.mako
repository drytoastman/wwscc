<%inherit file="/base.mako" />

<div id='content'>

<div id='serieslinks'>
%for s in sorted(c.activeSeries):
%if s == c.database:
<span>${s.upper()}</span> 
%else:
<a href='${h.url_for(database=s, action='')}'>${s.upper()}</a> 
%endif
%endfor
</div>

</div> <!-- content -->

