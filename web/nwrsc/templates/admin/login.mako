<%inherit file="base.mako" />
<h3>${c.request}</h3>
<form action='${h.url_for(action='login')}' method='POST'>
<label for='password'>Password</label><input type='password' name='password'/>
<input type='Submit' name='Submit' value='Submit'>
</form>
