<%inherit file="/admin/base.mako" />

<h3>Create New Series</h3>

<p>Create a new series, copying information from the current</p>

<form id="settingsForm" action="${c.action}" method="post">
<table class='form'>
		
<tr title="the new series file name">            
<th>Series File Name</th>
<td><input type="text" name="name" size="40" /></td>
</tr>
		
<tr title="the series password">            
<th>Password</th>
<td><input type="text" name="password" size="40" /></td>
</tr>

<tr title="copy settings over">
<th>Copy Settings</th>
<td><input type="checkbox" name="settings" checked /></td>
</tr>

<tr title="copy result and card templates over">
<th>Copy Templates</th>
<td><input type="checkbox" name="data" checked /></td>
</tr>

<tr>
<tr title="copy classes/indexes over">
<th>Copy Classes/Indexes</th>
<td><input type="checkbox" name="classes" checked /></td>
</tr>

<tr>
<tr title="copy drivers over">
<th>Copy Drivers</th>
<td><input type="checkbox" name="drivers" checked /></td>
</tr>

<tr>
<tr title="copy cars over, unless without drivers">
<th>Copy Cars</th>
<td><input type="checkbox" name="cars" checked /></td>
</tr>

<tr>
<tr title="create a prevlist from this series">
<th>Create PrevList</th>
<td><input type="checkbox" name="prevlist" /></td>
</tr>

<tr>
<td><input type="submit" value="Submit"/></td>
</tr>

</table>
</form>

