<%inherit file="/base.mako" />

<div id='content'>


<div class='tabcontainer'>
<div id='entrylist' class='tablist'>
<ul>
%for ev in c.events:
<li><a href='${h.url_for(action='view', other=ev.id)}'>${ev.name}</a></li>
%endfor
</ul>

<div class='tab'>
Entry Lists
</div>
</div>

<div id='serieslinks' class='tablist'>
<ul>
%for s in sorted(c.activeSeries):
%if s != c.database:
	<li><a href='${h.url_for(database=s, action='')}'>${s.upper()}</a></li>
%endif
%endfor
</ul>

<div class='tab'>
Other Series
</div>
</div>

</div> <!-- tabcontainer -->



<div id='header'>

<img id='seriesimg' src='${h.url_for(controller='db', name='seriesimage')}' alt='Series Image' />
%if c.settings.sponsorlink is not None and c.settings.sponsorlink.strip() != "":
  <a href='${c.settings.sponsorlink}' target='_blank'>
  <img id='sponsorimg' src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img id='sponsorimg' src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
%if c.settings:
<h2 id='seriesname'>${c.database.upper()} - ${c.settings.seriesname}</h2>
%endif
</div> <!-- header -->


<div class='errorlog'>
${c.previouserror}
</div>

${next.body()}

</div> <!-- content -->

