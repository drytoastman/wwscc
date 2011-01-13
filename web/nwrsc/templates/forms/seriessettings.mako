<%inherit file="/admin/base.mako" />

<h3>Series Settings</h3>

<form id="settingsForm" action="${c.action}" method="post">
<table class='form'>
		
<tr title="the name of the series">            
<th>Series Name</th>
<td><input type="text" name="seriesname" value="${c.settings['seriesname']}" size="40" /></td>
</tr>
		
<tr title="the series password">            
<th>Password</th>
<td><input type="text" name="password" value="${c.settings['password']}" size="40" /></td>
</tr>
		
<tr title="Largest car number to be available during preregistration">  
<th>Largest Car Number</th>
<td><input type="text" name="largestcarnumber" value="${c.settings.get(largestcarnumber, 1999)}" size="4"/></td>
</tr>

<tr title="Number of events required to be included in champ report">            
<th>Min Events</th>
<td><input type="text" name="minevents" value="${c.settings['minevents']}" size="4"/></td>
</tr>
		
<tr title="Number of events to use in championship calculation">            
<th>Best X Events</th>
<td><input type="text" name="useevents" value="${c.settings['useevents']}" size="4" /></td>
</tr>
		
<tr title="URL link for sponsor banner">            
<th>Sponsor Link</th>
<td><input type="text" name="sponsorlink" value="${c.settings['sponsorlink']}" size="40" /></td>
</tr>
		
<tr title="Ordering of points if using static points">            
<th>Points</th>
<td><input type="text" name="ppoints" value="${c.settings['ppoints']}" size="40" /></td>
</tr>

<tr title="Lock the database manually, not recommended">            
<th>Locked</th>
<td><input type="checkbox" name="locked" value="${c.settings['locked']}" /></td>
</tr>
		
<tr>
<td><input type="submit" value="Submit"/></td>
</tr>

</table>
</form>



<h3>Images</h3>

<form method=post enctype="multipart/form-data" action="${h.url_for(action='uploadimage')}">
<table class='form'>

<tr>
<th>Sponsor Image</th>
<td><input type=file name="sponsorimage"></td>
<td><img src='${h.url_for(controller='db', name='sponsorimage', eventid=None, action='nocache')}' height="70" /></td>
</tr>

<tr>
<th>Series Image</th>
<td><input type=file name="seriesimage"></td>
<td><img src='${h.url_for(controller='db', name='seriesimage', eventid=None, action='nocache')}' height="70" /></td>
</tr>

<tr>
<td><input type="submit" value="Submit"></td>
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


