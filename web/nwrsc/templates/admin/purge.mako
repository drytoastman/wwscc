<%inherit file="base.mako" />
<h3>Purge Using</h3>

<form action='${h.url_for(action='processPurge')}' method='post'>
%for f in c.files:
<input type='checkbox' name='${f}'/> ${f}<br/>
%endfor
<br/>
<input type='submit' value='Submit'/>
</form>
