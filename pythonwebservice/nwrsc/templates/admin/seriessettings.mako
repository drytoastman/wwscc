<%inherit file="base.mako" />

<h2>Series Settings</h2>

<form method=post enctype="multipart/form-data" action="${c.action}">
		
<div title="the name of the series">            
<span class='title'>Series Name</span>
<span class='input'><input type="text" name="seriesname" value="${c.settings.seriesname}" size="40" /></span>
</div>
		
<div title="the series password">            
<span class='title'>Password</span>
<span class='input'><input type="text" name="password" value="${c.settings.password}" size="40" /></span>
</div>
		
<div title="Largest car number to be available during preregistration">  
<span class='title'>Largest Car Number</span>
<span class='input'><input type="text" name="largestcarnumber" value="${c.settings.largestcarnumber}" size="4"/></span>
</div>

<div title="Number of events required to be included in champ report">            
<span class='title'>Min Events</span>
<span class='input'><input type="text" name="minevents" value="${c.settings.minevents}" size="4"/></span>
</div>

<div title="Use the best X events when calculating championship totals">
<span class='title'>Best X Events</span>
<span class='input'><input type="text" name="useevents" value="${c.settings.useevents}" size="4" /></span>
</div>
		
<div title="URL link for sponsor banner">            
<span class='title'>Sponsor Link</span>
<span class='input'><input type="text" name="sponsorlink" value="${c.settings.sponsorlink}" size="40" /></span>
</div>

<div title="Additional sorting options for determing championship in case of tie, comma separated list of (firsts, seconds, thirds, fourths, attended)">
<span class='title'>Championship TieBreakers</span>
<span class='input'><input type="text" name="champsorting" value="${c.settings.champsorting}" size="40" /></span>
</div>

<div title="A globally applied street tire multiplier used for cars marked as such.  If 1.0, option is not visible to users.">
<span class='title'>Global Tire Index</span>
<span class='input'><input type="text" name="globaltireindex" value="${c.settings.globaltireindex}" size="40" /></span>
</div>

<div title="Use position to determine points rather than difference from first">
<span class='title'>Use Position for Points</span>
<span class='input'><input type="checkbox" name="usepospoints" ${c.settings.usepospoints and "checked"} /></span>
</div>
		
<div id='ppointrow' title="Ordering of points if using static points">            
<span class='title'></span>
<span class='input'><input type="text" name="pospointlist" value="${c.settings.pospointlist}" size="40" /></span>
</div>

<div title="Apply indexes after penalties, default is false (index is applied, then penalties)">
<span class='title'>Apply Index After Penalties</span>
<span class='input'><input type="checkbox" name="indexafterpenalties" ${c.settings.indexafterpenalties and "checked"} /></span>
</div>

<div title="Unique numbers across all classes, not each class individually">            
<span class='title'>Series Wide Numbers</span>
<span class='input'><input type="checkbox" name="superuniquenumbers" ${c.settings.superuniquenumbers and "checked"} /></span>
</div>
		
<div title="Lock the database manually, not recommended">            
<span class='title'>Locked</span>
<span class='input'><input type="checkbox" name="locked" ${c.settings.locked and "checked"} /></span>
</div>
		
<div title="Image used at the top of the registration site">
<span class='title'>Sponsor Image</span>
<span class='imageinfo'>No image</span>
<input type="checkbox" name="blanksponsorimage"/>Blank
<input type="file" class="file" name="sponsorimage">
<img src='${h.url_for(controller='db', name='sponsorimage', eventid=None, action='nocache')}'/>
</div>

<div title="Image used at the top of the registration site">
<span class='title'>Series Image</span>
<span class='imageinfo'>No image</span>
<input type="checkbox" name="blankseriesimage"/>Blank
<input type="file" class="file" name="seriesimage">
<img src='${h.url_for(controller='db', name='seriesimage', eventid=None, action='nocache')}'/>
</div>

<div title="Image passed to the card template for printing cards">
<span class='title'>Card Image</span>
<span class='imageinfo'>No image</span>
<input type="checkbox" name="blankcardimage"/>Blank
<input type="file" class="file" name="cardimage"/>
<img src='${h.url_for(controller='db', name='cardimage', eventid=None, action='nocache')}'/>
</div>

<div>
<span class='title'>Templates</span>
<a title="The template used to display the results for the classes" target='_blank' href='${h.url_for(action='editor', name='classresult.mako')}'>ClassTables</a>
<a title="The template used to display the toptimes" target='_blank' href='${h.url_for(action='editor', name='toptimes.mako')}'>TopTimes</a>
<a title="The template used to for event posting, can use class tables and toptimes" target='_blank' href='${h.url_for(action='editor', name='event.mako')}'>Event</a>
<a title="The template used for the championship standing report" target='_blank' href='${h.url_for(action='editor', name='champ.mako')}'>Championship</a>
<a title="The stylesheet that is supplied with any page loaded from the results pages" target='_blank' href='${h.url_for(action='editor', name='results.css')}'>Stylesheet</a>
<a title="The python code used to create a single timing card" target='_blank' href='${h.url_for(action='editor', name='card.py')}'>Timing Cards</a>
</div>

<div style='margin-top:10px; padding-left:80px;'>
<span class='input'><input type="submit" value="Submit"></span>
</div>

</form>


<script type='text/javascript'>
$(window).load(function(){
	// wait until image loads before updating text info
	$(".imageinfo").each(function() {
		var img = $(this).siblings('img');
		if (img.width() > 0) {
			$(this).text(img.width() + "w x " + img.height() + "h");
		}
	});
});

$(document).ready(function(){
	// ppoint row hiding
	$("#ppointrow").toggle(${int(c.settings.usepospoints)}==1);
	$("input[type=checkbox][name=usepospoints]").click(function() { $("#ppointrow").toggle(this.checked); });

	// image display when hovering
	$('form img').hide().css({"position":"absolute", "border":"1px solid black"});
	$('.imageinfo').mousemove(function(event) {
		var y = $(this).siblings('img').height() + 10;
		$(this).siblings('img').show().css({"top":event.pageY-y, "left":event.pageX});
	}).mouseleave(function() {
		$(this).siblings('img').hide();
	});

	// make anchors into buttons
	$('form a[target=_blank]').button().css("font-size", "0.7em");
});
</script>

<style type='text/css'>
	span.title { display: inline-block;  width: 200px; height: 20px; text-align:right; margin-right: 4px; font-weight: bold; }
	span.imageinfo { display: inline-block; width: 80px; }
	form a, form img { vertical-align: middle; }
	input.file { font-size: 0.7em; }
</style>


