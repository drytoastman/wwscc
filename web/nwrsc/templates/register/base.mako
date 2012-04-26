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

<style>
#header img { max-height: 70px; }
</style>

<div id='header'>

<img id='seriesimg' src='${h.url_for(controller='db', name='seriesimage')}' alt='Series Image' />

<div id='sponsor'>
%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>

<h2 id='seriesname'>${c.database.upper()} - ${c.settings.seriesname}</h2>

</div> <!-- header -->

<div id='hrule'></div>

%if c.previouserror:
<div class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror}</span>
</div>
%endif

${next.body()}

</div> <!-- content -->

