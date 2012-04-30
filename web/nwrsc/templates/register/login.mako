<%inherit file="base.mako" />
<%namespace file="/forms/driverform.mako" import="driverform"/>

<div class='helpbox'>
If you have a profile in this series, please login.
If you have a profile in another active series, use that information and you will be given the option of copying the profile.
Otherwise, create a new profile.
</div>

<table id='loginrow'>
<tr>
<td id='logincell'>
<form id="loginForm" action="${h.url_for(action='checklogin')}" method="post">
<div id='logintable'>
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
</td>

<td id='orcell'>
OR
</td>

<td id='othercell'>
<ul>
%for name, creds in c.otherseries.iteritems():
<li><button onclick="copylogin('${creds.firstname}', '${creds.lastname}', '${creds.email}', '${name}')">Copy Profile From ${name.upper()}</button></li>
%endfor
<li><button onclick='editdriver(-1)'>Create New Profile</button></li>
</ul>
</td>

</tr>
</table>

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

