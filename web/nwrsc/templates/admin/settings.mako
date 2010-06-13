<%inherit file="base.mako" />
<h3>Series Settings</h3>
<% from nwrsc.forms import settingsForm %>
${settingsForm(action=h.url_for(action=c.action), value=c.settings)|n}

<br/>
<h3>Images</h3>
<form method=post enctype="multipart/form-data" action="${h.url_for(action='uploadimage')}">
<table>

<tr class='odd'>
<td class='labelcol'><label>Sponsor Image</label></td>
<td><input type=file name="sponsorimage">
<td><img src='${h.url_for(controller='db', name='sponsorimage', eventid=None, action='nocache')}' height="70" /></td>
</tr>


<tr class='even'>
<td class='labelcol'><label>Series Image</label></td>
<td><input type=file name="seriesimage">
<td><img src='${h.url_for(controller='db', name='seriesimage', eventid=None, action='nocache')}' height="70" /></td>
</tr>
<tr class='odd,lastrow'>
<td></td>
<td>
<input type=submit value="Submit">
</td>
</tr>
</table>
</form>


<br/>
<h3>Template Code</h3>
<ul id='templatelist'>
<li><a target='_blank' href='${h.url_for(action='editor', name='classresult.mako')}'>Class Tables</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='toptimes.mako')}'>Top Times List</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='event.mako')}'>Event Template</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='champ.mako')}'>Championship Template</a>
<br/>
<li><a target='_blank' href='${h.url_for(action='editor', name='results.css')}'>Results Stylesheet</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='card.py')}'>Timing Cards</a>
</ul>

