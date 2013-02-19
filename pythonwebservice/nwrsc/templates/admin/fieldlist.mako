<%inherit file="base.mako" />
<% from nwrsc.model import DriverField %>

<% def singleline(text): return text.replace('\n', '') %>

<%def name="dorow(ii, field)" filter="singleline">
<tr data-counter="${ii}">
<td><input type="text" name="fieldlist-${ii}.name" value="${field.name}" size="20" /></td>
<td><input type="text" name="fieldlist-${ii}.title" value="${field.title}" size="20" /></td>
<!-- <td><input type="text" name="fieldlist-${ii}.type" value="${field.type}" size="6" /></td> -->
<td><button class="small deleterow">Del</button></td>
</tr>
</%def>

<h2>Driver Fields Editor</h2>

<p>
Any extra fields in this list will be available as extra fields for each driver entry.<br/>
Eventually there may be a field type and data validation but not yet.
</p>

<table>
<tr><th>Name</th><td>The field name stored in the database</td></tr>
<tr><th>Title</th><td>The field title as displayed in dialogs or forms</td></tr>
</table>

<p></p>


<form action="${c.action}" method="post" id="fieldlistform">
<table class="fieldtable">
<tr>
<th>Name</th>
<th>Title</th>
<!-- <th>Type</th> -->
</tr>

<% ii = 0 %>
%for ii, field in enumerate(c.fieldlist):
${dorow(ii, field)}
%endfor

</table>
<button class='addbutton'>Add</button>
<input type='submit' value="Save">
</form>

<table class='ui-helper-hidden' id='fieldlisttemplate'>
${dorow('xxxxx', DriverField())}
</table>

<script>
$(document).ready(function(){
	$('.deleterow').click(function() { $(this).closest('tr').remove(); return false; });
	$('#fieldlistform .addbutton').click(function() {
        newCountedRow('#fieldlistform', '#fieldlisttemplate');
        return false;
    });
});
</script>
