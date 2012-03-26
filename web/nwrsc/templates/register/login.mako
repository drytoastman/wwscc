<%inherit file="/base.mako" />
<%namespace file="/forms/driverform.mako" import="driverform"/>

<style>
#series { margin-left: 20px; }
#sponsorimg { margin-left: 20px; display: block; }
#login { margin-left: 20px; margin-top: 20px; }
#login th { text-align: right; }
#submit { margin-left: 200px; margin-top: 10px; font-size: 0.8em; }
</style>


<div id='series'>
<h2>${c.database} - ${c.settings.seriesname}</h2>
</div>


<div id='sponsorimg'>
%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>


%if len(c.previouserror) > 0:
<div id='errormsg' class='ui-state-error'>
<span class='ui-state-error-text'>${c.previouserror|n}</span>
</div>
%endif


<form id="loginForm" action="${h.url_for(action='checklogin')}" method="post">
<div id='login'>
<input type="hidden" name="forward" value=""/>
<table class='form'>
<tr><th>First Name</th><td><input type="text" name="firstname" value="" class="required"/></td></tr>
<tr><th>Last Name</th><td><input type="text" name="lastname" value="" class="required"/></td></tr>
<tr><th>Email or Unique Id</th><td><input type="text" name="email" value="" class="required"/></td></tr>
</table>
</div>

<div id='submit'>
<input type="submit" value="Login" id='loginsubmit'/>
<button id='newbutton'>New Driver</button>
</div>
</form>

${driverform(action=h.url_for(action='newprofile'), method='POST')}

<script>
$(document).ready(function() {
	drivers=Array();
	$("#loginForm").validate(); 
	$("#loginsubmit").button();
	$("#newbutton").button().click(function() { editdriver(-1); return false; } );
	setupDriverDialog("New Driver");
});

function driveredited()
{
	$("#drivereditor").submit();
}

</script>

