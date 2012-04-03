<%inherit file="base.mako" />
<%namespace file="/forms/driverform.mako" import="driverform"/>

<style>
#login { margin-left: 20px; margin-top: 20px; }
#login th { text-align: right; }
#loginsubmit { margin-left: 230px; margin-top: 10px; font-size: 0.8em; }
</style>


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
</div>
</form>

<!--<button id='newbutton'>New Driver</button>-->
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

