<%inherit file="base.mako" />
<%namespace file="/forms/driverform.mako" import="driverform"/>

<div class='helpbox'>
If you have a profile in this series, please login.
If you have a profile in another active series, use that information and you will be given the option of copying the profile.
Otherwise, create a new profile.  If you are just looking for entry lists, <a href='${h.url_for(action="view")}'>click here</a>.
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
<li>
	<button class='copylogin' data-creds='{"firstname":"${creds.firstname}", "lastname":"${creds.lastname}", "email":"${creds.email}", "series":"${name}"}'>
		Copy Profile From ${name.upper()}
	</button>
</li>
%endfor
<li><button class='createdriver'>Create New Profile</button></li>
</ul>
</td>

</tr>
</table>

${driverform(action=h.url_for(action='newprofile'), method='post')}

