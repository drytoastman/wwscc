<%inherit file="base.mako" />
<h3>Series Settings</h3>
<% from nwrsc.forms import settingsForm %>
${settingsForm(action=h.url_for(action=c.action), value=c.settings)|n}

<h3>Images</h3>
<form method=post enctype="multipart/form-data" action="${h.url_for(action='uploadimage')}">
<table>
<tr>
<td><b>Sponsor Image</b><br/><input type=file name="sponsorimage"></td>
<td><img src='${h.url_for(controller='db', name='sponsorimage', eventid=None, action='nocache')}'/></td>
</tr>
<tr>
<td><b>Series Image</b><br/><input type=file name="seriesimage"></td>
<td><img src='${h.url_for(controller='db', name='seriesimage', eventid=None, action='nocache')}'/></td>
</tr>
</table>
<input type=submit value="Upload">
</form>


<h3>Template Code</h3>
<ul>
<li><a target='_blank' href='${h.url_for(action='editor', name='classresult.mako')}'>Class Tables</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='toptimes.mako')}'>Top Times List</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='event.mako')}'>Event Template</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='champ.mako')}'>Championship Template</a>
<br/>
<li><a target='_blank' href='${h.url_for(action='editor', name='results.css')}'>Results Stylesheet</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='card.py')}'>Timing Cards</a>
</ul>

