<%inherit file="base.mako" />

<h2>Series Settings</h2>

<form method=post enctype="multipart/form-data" action="${c.action}">
<table class='form'>
		
<tr title="the name of the series">            
<th>Series Name</th>
<td><input type="text" name="seriesname" value="${c.settings.seriesname}" size="40" /></td>
</tr>
		
<tr title="the series password">            
<th>Password</th>
<td><input type="text" name="password" value="${c.settings.password}" size="40" /></td>
</tr>
		
<tr title="Largest car number to be available during preregistration">  
<th>Largest Car Number</th>
<td><input type="text" name="largestcarnumber" value="${c.settings.largestcarnumber}" size="4"/></td>
</tr>

<tr title="Number of events required to be included in champ report">            
<th>Min Events</th>
<td><input type="text" name="minevents" value="${c.settings.minevents}" size="4"/></td>
</tr>
		
<tr title="Number of events to use in championship calculation">            
<th>Best X Events</th>
<td><input type="text" name="useevents" value="${c.settings.useevents}" size="4" /></td>
</tr>
		
<tr title="URL link for sponsor banner">            
<th>Sponsor Link</th>
<td><input type="text" name="sponsorlink" value="${c.settings.sponsorlink}" size="40" /></td>
</tr>


<tr title="Additional sorting options for determing championship in case of tie, comma separated list of (firsts, seconds, thirds, fourths, attended)">
<th>Championship TieBreakers</th>
<td><input type="text" name="champsorting" value="${c.settings.champsorting}" size="40" /></td>
</tr>


<tr title="Use position to determine points rather than difference from first">
<th>Use Position for Points</th>
<td><input type="checkbox" name="usepospoints" ${c.settings.usepospoints and "checked"} onclick='$("#ppointrow").toggle(this.checked);' /></td>
</tr>
		
<tr id='ppointrow' title="Ordering of points if using static points">            
<th></th>
<td><input type="text" name="pospointlist" value="${c.settings.pospointlist}" size="40" /></td>
</tr>

<script type='text/javascript'>
$(document).ready(function(){ $("#ppointrow").toggle(${int(c.settings.usepospoints)}==1); });
</script>


<tr title="Apply indexes after penalties, default is false (index is applied, then penalties)">
<th>Apply Index After Penalties</th>
<td><input type="checkbox" name="indexafterpenalties" ${c.settings.indexafterpenalties and "checked"} /></td>
</tr>

<tr title="Unique numbers across all classes, not each class individually">            
<th>Series Wide Numbers</th>
<td><input type="checkbox" name="superuniquenumbers" ${c.settings.superuniquenumbers and "checked"} /></td>
</tr>
		
<tr title="Lock the database manually, not recommended">            
<th>Locked</th>
<td><input type="checkbox" name="locked" ${c.settings.locked and "checked"} /></td>
</tr>
		
<tr title="Image used at the top of the registration site">
<th>Sponsor Image</th>
<td><input type=file name="sponsorimage"></td>
<td><img src='${h.url_for(controller='db', name='sponsorimage', eventid=None, action='nocache')}' height="40" /></td>
</tr>

<tr title="Image used at the top of the registration site">
<th>Series Image</th>
<td><input type=file name="seriesimage"></td>
<td><img src='${h.url_for(controller='db', name='seriesimage', eventid=None, action='nocache')}' height="40" /></td>
</tr>

<tr title="Image passed to the card template for printing cards">
<th>Card Image</th>
<td><input type=file name="cardimage"></td>
<td><img src='${h.url_for(controller='db', name='cardimage', eventid=None, action='nocache')}' height="40" /></td>
</tr>

<tr>
<td><input type="submit" value="Submit"></td>
</tr>

</table>
</form>


<h2>Template Code</h2>
<ul id='templatelist'>
<li><a target='_blank' href='${h.url_for(action='editor', name='classresult.mako')}'>Class Tables</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='toptimes.mako')}'>Top Times List</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='event.mako')}'>Event Template</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='champ.mako')}'>Championship Template</a>
<br/>
<li><a target='_blank' href='${h.url_for(action='editor', name='results.css')}'>Results Stylesheet</a>
<li><a target='_blank' href='${h.url_for(action='editor', name='card.py')}'>Timing Cards</a>
</ul>


