<%inherit file="/base.mako" />
<div id='nav'>
<p><img src='${h.url_for(controller='db', name='seriesimage')}' alt='Series Image' /></p>
<p id='currentuser'>
%if c.driverid:
Current: ${c.firstname} ${c.lastname}
%endif
</p>
<p/>
<a/>
<a class='tab ${c.tabflags.get('event', '')}' href='${h.url_for(action='events')}'>Events</a>
<a class='tab ${c.tabflags.get('cars', '')}' href='${h.url_for(action='cars')}'>My Cars</a>
<a class='tab ${c.tabflags.get('profile', '')}' href='${h.url_for(action='profile')}'>My Profile</a>
<a class='tab ${c.tabflags.get('entries', '')}' href='${h.url_for(action='view')}'>View Entries</a>
<a class='tab ${c.tabflags.get('logout', '')}' href='${h.url_for(action='logout')}'>Logout</a>
</div>

<div id='contentpane'>
<div id='sponser'>

%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif

</div>

<div id='error'>
${c.previouserror|n}
</div>

<div id='content'>
${next.body()}
</div>
</div>
