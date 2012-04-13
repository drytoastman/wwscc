<%inherit file="base.mako" />
<%namespace file="/forms/driverform.mako" import="driverform"/>

<style>
#login { margin-left: 20px; margin-top: 20px; }
#login th { text-align: right; }
#loginsubmit { margin-left: 230px; margin-top: 10px; font-size: 0.8em; }

#loginoptions { margin-top: 10px; margin-left: 40px; margin-bottom: 30px; }
#loginoptions ul { list-style: none; }
#loginoptions li { padding: 3px; }
#loginoptions li button { width: 160px; text-align: left; }
#loginoptions span { display: block; }
#loginoptions div { margin-bottom: 10px; }
#loginoptions button { font-size: 0.7em !important; }
</style>

<div id='loginoptions' class='ui-state-error-text'>
%if c.otherseries:
<span>Provided name and email not found in this series, however it was found in another active series.</span>
<span>You can click one of the matching series to copy that profile over and login.</span>
<span>Or you can create a new profile if needed.</span>
<ul>
%for s in c.otherseries:
<li><button onclick="copylogin('${s.driver.firstname}', '${s.driver.lastname}', '${s.driver.email}', '${s.name}')">Copy From ${s.name}</button></li>
%endfor
<li><button onclick="editdriverdirect('${s.driver.firstname}', '${s.driver.lastname}', '${s.driver.email}')">New Profile</button></li>
</ul>

%elif c.shownewprofile:

<div>Unable to find a match, please try again.  If you have never registered before you can create a </div>
<button onclick='editdriver(-1)'>New Profile</button>
%endif
</div>

<form id="loginForm" action="${h.url_for(action='checklogin')}" method="post">
<div id='login'>
<input type="hidden" name="otherseries" value=""/>
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
	$("button").button();
	setupDriverDialog("New Driver");
});

function driveredited()
{
	$("#drivereditor").submit();
}

function copylogin(f, l, e, s)
{
	$("#loginForm [name=firstname]").val(f);
	$("#loginForm [name=lastname]").val(l);
	$("#loginForm [name=email]").val(e);
	$("#loginForm [name=otherseries]").val(s);
	$("#loginForm").submit();
}


</script>

