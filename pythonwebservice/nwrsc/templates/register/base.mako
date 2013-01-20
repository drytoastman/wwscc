<%inherit file="/base.mako" />

<div id='content'>

<div id='serieslinks'>
%for s in sorted(c.activeSeries):
%if s != c.database:
	<a href='${h.url_for(database=s, action='')}'>${s.upper()}</a> 
%endif
%endfor
</div>

<div id='seriestab' class='ui-widget ui-state-default'>
Other Series
</div>

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

<!--<div id='hrule'></div>-->

%if c.previouserror:
<div class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror}</span>
</div>
%endif

${next.body()}

</div> <!-- content -->

<script type='text/javascript'>
$(document).ready(function() {
	$('#serieslinks').css('display', 'none');
	$('#seriestab').click(function() { $('#serieslinks').toggle('blind'); });
});
</script>

