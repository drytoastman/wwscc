<%inherit file="/base.mako" />

<h2 style='margin-left:20px' id='seriesname'>${c.settings.seriesname}</h2>

<div id='sponsor'>
%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>

<div id='errormsg'>
${c.previouserror|n}
</div>

<div id='content'>
${next.body()}
</div>

