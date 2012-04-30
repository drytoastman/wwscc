<%inherit file="/base.mako" />

<div id='content'>

<div id='serieslinks'>
<a style='font-size:0.8em;' href='${h.url_for(controller='registerold', database=None)}'>[Old Site]</a> 
%for s in sorted(c.activeSeries):
%if s == c.database:
<span>${s.upper()}</span> 
%else:
<a href='${h.url_for(database=s, action='')}'>${s.upper()}</a> 
%endif
%endfor
</div>

</div> <!-- content -->

