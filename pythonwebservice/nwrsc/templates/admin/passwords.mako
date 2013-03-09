<%inherit file="base.mako" />

<h2>Password Update</h2>
<p>
Enter new passwords, blank fields will be ignored.<br/>
Event passwords are only necessary for allowing constrained access to others.  The series password will work everywhere.
</p>

<form class='passwordform' method='POST' action='${c.action}'>
<table>
<tr>
<td><label class='series' for='series'>Series</label></td>
<td><input type='text' size='30' id='series' name='series'/></td>
</tr>

%for event in c.events:
<tr>
<td><label for='e${event.id}'>${event.name}</label></td>
<td><input type='text' size='30' id='e${event.id}' name='${event.id}'/></td>
</tr>
%endfor
</table>
	
<input type='submit' value='submit'>
</form>
