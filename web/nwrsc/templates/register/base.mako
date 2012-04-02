<%inherit file="/base.mako" />
<%namespace file="/forms/carform.mako" import="carform"/>
<%namespace file="/forms/driverform.mako" import="driverform"/>
<%namespace file="events.mako" import="eventdisplay"/>
<%namespace file="cars.mako" import="carlist"/>
<%namespace file="profile.mako" import="profile"/>
<%def name="leftpiece()"></%def>

<div id='content'>

<div id='series'>
<h2>${c.database} - ${c.settings.seriesname}</h2>
</div>

${self.leftpiece()}

<div id='sponsor'>
%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>


<div id='beforeerror'></div>

%if len(c.previouserror) > 0:
<div id='errormsg' class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror|n}</span>
</div>
%endif

${next.body()}

</div>

