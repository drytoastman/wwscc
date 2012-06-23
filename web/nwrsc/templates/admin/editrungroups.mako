<%inherit file="base.mako" />

<style type="text/css">
.rungroup {
	list-style-type: none; 
	margin: 0; 
	padding: 5px; 
	width: 55px; 
	margin-right: 25px; 
	border: 1px 
	solid #888; 
}

.rungroup li { 
	margin: 3px; 
	padding: 4px; 
	width: 40px; 
	font-size: 0.75em; 
}

h4 { 
	margin-top: 0px; 
	margin-bottom: 6px; 
}

div.grouping { 
	float: left; 
}

.ack {
	background: #F00 !important;
}
</style>

<script type="text/javascript">
$(function() {
	$(".rungroup").sortable({
		connectWith: '.rungroup',
		update: function(event, ui) { ui.item.addClass("ui-state-highlight"); }
	}).disableSelection();
	$("#sendgroups").button();
});

function collectgroups(frm)
{
	for (var ii = 0; ii < 3; ii++)
	{
		var x = Array();
		$("#group"+ii+" li").each(function() {
			x.push(this.innerHTML);
		});
		frm['group'+ii].value = ""+x;
	}
}

</script>

<h3>RunGroup Editor</h3>

<form method="post" action="${c.action}" onSubmit="collectgroups(this);">

%for group in range(3):
<div class='grouping'>
<h4> Group ${group} </h4>
<ul id="group${group}" class="rungroup ui-corner-all">
%for code in c.groups.get(group, []):
	<li class="ui-state-default ui-corner-all">${code}</li>
%endfor
</ul>
<input type='hidden' name='group${group}' value=''/>
</div>
%endfor


<br clear="both"/>
<br/>
<input type='submit' id='sendgroups' value='Submit'/>

</form>


