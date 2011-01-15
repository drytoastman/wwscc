<%inherit file="/admin/base.mako" />

<% def singleline(text): return text.replace('\n', '') %>

<%def name="dorow(ii, cls)" filter="singleline">
<tr>
<td>
	<input type="text" name="clslist-${ii}.code" value="${cls.code}" size="6" />
</td>
<td>
	<input type="text" name="clslist-${ii}.descrip" value="${cls.descrip}" size="50" />
</td>
<td title="Receives trophies at events">
	<input type="checkbox" name="clslist-${ii}.eventtrophy" ${cls.eventtrophy and 'checked="yes"' or ''|n} />
</td>
<td title="Receives trophies for the series">
	<input type="checkbox" name="clslist-${ii}.champtrophy" ${cls.champtrophy and 'checked="yes"' or ''|n} />
</td>
<td title="Cars are individually indexed by index value">
	<input type="checkbox" name="clslist-${ii}.carindexed" ${cls.carindexed and 'checked="yes"' or ''|n} />
</td>
<td title="Entire class is indexed matching class code to an index code">
	<input type="checkbox" name="clslist-${ii}.classindexed" ${cls.classindexed and 'checked="yes"' or ''|n}  />
</td>
<td title="This multiplier is applied to entire class, i.e. street tire factor">
	<input type="text" name="clslist-${ii}.classmultiplier" value="${"%0.3f" % (cls.classmultiplier or 1.000)}" size="5" />
</td>
<td title="Limit number of counted runs for this class">
	<input type="text" name="clslist-${ii}.countedruns" value="${cls.countedruns or 0}" size="3" />
</td>
<td>
	<button class="small deleterow">Del</button>
</td>
<td>
	<input type="hidden" name="clslist-${ii}.numorder" value="${cls.numorder}" />
</td>
</tr>
</%def>

<h3>Class Editor</h3>

<form action="${c.action}" method="post">
<table id='classtable'>
<tr>
<th>Code</th>
<th>Description</th>
<th>Event<br/>Trophy</th>
<th>Champ<br/>Trophy</th>
<th>Cars<br/>Indexed</th>
<th>Class<br/>Indexed</th>
<th>Addl<br/>Multiplier</th>
<th>Counted<br/>Runs</th>
</tr>

%for ii, cls in enumerate(c.classlist):
${dorow(ii, cls)}
%endfor

</table>
<button id='addbutton'>Add</button>
<input type='submit' value="Save">
</form>

<%  from nwrsc.model import Class %>
<script>
var rowstr = '${dorow('xxxxx', Class())}\n';
var ii = ${ii};
$('#addbutton').click(function() {
	$("#classtable > tbody").append(rowstr.replace(/xxxxx/g, ++ii));
	$('#classtable button').button();
	return false;
});
</script>

