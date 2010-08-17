<%inherit file="/admin/base.mako" />

<h3>Index Editor</h3>

<form action="${c.action}" method="post">
<table id='indextable'>
<tr>
<th>Code</th>
<th>Description</th>
<th>Value</th>
</tr>

%for ii, idx in enumerate(c.indexlist):
<tr>
<td><input type="text" name="idxlist-${ii}.code" value="${idx.code}" size="6" /></td>
<td><input type="text" name="idxlist-${ii}.descrip" value="${idx.descrip}" size="50" /></td>
<td><input type="text" name="idxlist-${ii}.value" value="${"%0.3f" % idx.value}" size="5" /></td>
<td><button class='small deleterow'>Del</button></td>
</tr>
%endfor

</table>
<button id='addbutton'>Add</button>
<input type='submit' value="Save">
</form>

<script>
var rowstr = '<tr><td><input type="text" name="idxlist-xxxxx.code" size="6" /></td><td><input type="text" name="idxlist-xxxxx.descrip" size="50" /></td><td><input type="text" name="idxlist-xxxxx.value" size="5" /></td><td><button class="small deleterow">Del</button></td></tr>\n';
var ii = ${ii};
$('#addbutton').click(function() {
	$("#indextable > tbody").append(rowstr.replace(/xxxxx/g, ++ii));
	$('#indextable button').button();
	return false;
});
</script>

