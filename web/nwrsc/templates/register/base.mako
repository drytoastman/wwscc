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

<div id='header'>

<div id='seriesimg'>
<img src='${h.url_for(controller='db', name='seriesimage')}' alt='Series Image' />
</div>

<div id='sponsorimg'>
%if c.settings.sponsorlink is not None and c.settings.sponsorlink.strip() != "":
  <a href='${c.settings.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>

%if c.settings:
<h2 id='seriesname'>${c.database.upper()} Registration - ${c.settings.seriesname}</h2>
%endif

</div> <!-- header -->

<div id='hrule'></div>

%if c.previouserror:
<div class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror}</span>
</div>
%endif

${next.body()}

</div> <!-- content -->

