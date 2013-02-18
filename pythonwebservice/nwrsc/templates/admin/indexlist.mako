<%inherit file="base.mako" />

<h2>Index Editor</h2>

<form action="${c.action}" method="post" id='indexlistform'>
<table class='indextable'>
<tr>
<th>Code</th>
<th>Description</th>
<th>Value</th>
</tr>

%for ii, idx in enumerate(c.indexlist):
<tr data-counter="${ii}">
<td><input type="text" name="idxlist-${ii}.code" value="${idx.code.strip()}" size="6" /></td>
<td><input type="text" name="idxlist-${ii}.descrip" value="${idx.descrip.strip()}" size="50" /></td>
<td><input type="text" name="idxlist-${ii}.value" value="${"%0.3f" % idx.value}" size="5" /></td>
<td><button class='small deleterow'>Del</button></td>
</tr>
%endfor

</table>
<button class='addbutton'>Add</button>
<button class='getbutton'>Raw Text Load</button>
<input type='submit' value="Save">
</form>


<table class='ui-helper-hidden' id='indexlisttemplate'>
<tr>
<td><input type="text" name="idxlist-xxxxx.code" size="6" /></td>
<td><input type="text" name="idxlist-xxxxx.descrip" size="50" /></td>
<td><input type="text" name="idxlist-xxxxx.value" size="5" /></td>
<td><button class="small deleterow">Del</button></td>
</tr>
</table>

<script>
$(document).ready(function(){
	$('#indexlistform .addbutton').click(function() {
        newCountedRow('#indexlistform', '#indexlisttemplate');
        return false;
    });

	$('#indexlistform .getbutton').click(function() {
		var data = window.prompt("Enter raw text", "");
		var items = data.split(/\s+/);
		var lastindex = ""
		var skipped = Array();
		for (var ii = 0; ii < items.length; ii++) {
			var val = parseFloat(items[ii]);
			if (isNaN(val)) {
				lastindex = items[ii];
			} else {
				var elem = $('input[value="'+lastindex+'"]');
				if (elem.length == 0) {
					skipped.push(lastindex);
				} else {
					elem.parents('tr').find('[name$="value"]').val(val).css('background', 'yellow');
				}
			}
		}

		if (skipped.length > 0) {
			alert("Couldn't find " + skipped + ".  They were skipped.");
		}
        return false;
    });
});
</script>
