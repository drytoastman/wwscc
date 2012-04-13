<%inherit file="/base.mako" />

<div id='content'>

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

<h2 id='seriesname'>${c.database} - ${c.settings.seriesname}</h2>

</div> <!-- header -->

<div id='hrule'></div>

%if c.previouserror:
<div class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror}</span>
</div>
%endif

${next.body()}

</div> <!-- content -->

