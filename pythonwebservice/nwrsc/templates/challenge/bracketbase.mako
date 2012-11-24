<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type="text/css">
#popup {
	display:none;
	position: absolute; 
	background: white; 
	font-size: 0.8em;
	border: 3px ridge #999999;
}
#popup td { white-space:nowrap;}
img { border: none; }
tr { background: #FAFAFA; }
tr.winner { background: #EE8; }
td { padding-left: 8px; padding-left: 8px; }
p.winner { text-align:center; font-size: 1.2em; margin: 0px; background: #EEE; }
span.dial { font-weight: bold; }
table { margin: 0px; }
</style>

<script type="text/javascript">
function mousein(ev, rnd)
{	
	pop = document.getElementById("popup");
	pop.style.left = (ev.clientX-10)+"px";
	pop.style.top = (ev.clientY-10)+"px";
	pop.style.display = 'block';
	pop.innerHTML = "<h4>loading round data...</h4>";
	$('#popup').load('${h.url_for(action='round', id=c.cid)}&round='+rnd);
}
</script>
</%def>

<img src="${h.url_for(action='bracketimg', id=c.cid)}" alt="loading bracket image..." usemap="#bracketmap"/>
<map name="bracketmap" id="bracketmap">
%for rnd, coord in c.coords:
  <area shape="rect" coords="${coord}" href="#" onclick="mousein(event, ${rnd});" alt="Round${rnd}" />
%endfor
</map>
<div id='popup' onclick='style.display="none";'></div>
