<%def name="careditor()">

<script>
function classchange()
{
	indexbox = $("#indexcode");
	$("#number").val('');
	$("#displaynumber").html('');

	if ($("#classcode option:selected").attr('indexed'))
	{
		$('#indexcode').removeAttr('disabled');
		$("#myselect option:eq(0)").attr("selected", "selected");
	}
	else
	{
		$('#indexcode').attr('disabled', 'disabled');
	}

	//adjustAvailableLink();
}

function editcar(cid, action)
{
	$('#careditor #carid').val(cid);
	$('#careditor #year').val(cars[cid].year);
	$('#careditor #make').val(cars[cid].make);
	$('#careditor #model').val(cars[cid].model);
	$('#careditor #color').val(cars[cid].color);
	$('#careditor #classcode').val(cars[cid].classcode);
	$('#careditor #indexcode').val(cars[cid].indexcode);
	classchange();
	$('#careditor #number').val(cars[cid].number);
	$('#careditor #displaynumber').html(cars[cid].number);
	$('#careditor').dialog('open');
	// open editor
}

$(document).ready(function(){
    $("#careditor").validate({
		rules: {
			indexcode: {
				required: function(element) { return ($("#classcode option:selected").attr('indexed') == '1'); },
			}
		},
		messages: { 
			indexcode: 'an index is required for indexed classes'
		},
	});

	$("#careditor").dialog({
		autoOpen: false,
		height: 350,
		width: 450,
		modal: true,
		title: 'Car Editor',
		buttons: {
			'Ok': function() {
				if ($("#careditor").valid()) {
					$(this).dialog('close');
					caredited();
				}
			},
			Cancel: function() { $(this).dialog('close'); }
		},
		close: function() {
		}
	});
});
</script>


<form id='careditor'>
<input id='carid' name='carid' type='hidden'/>
<table class='careditor'>
<tbody>
<tr><th>Year</th>  <td><input id='year'   name='year'   type='text'/></td></tr>
<tr><th>Make</th>  <td><input id='make'   name='make'   type='text'/></td></tr>
<tr><th>Model</th> <td><input id='model'  name='model'  type='text'/></td></tr>
<tr><th>Color</th> <td><input id='color'  name='color'  type='text'/></td></tr>
<tr><th>Class</th> <td>
<select id='classcode' name='classcode' onchange='classchange();'>
<%
for code in sorted(c.classdata.classlist):
	cls = c.classdata.classlist[code]
	context.write("<option value='%s' " % (cls.code))
	if cls.carindexed:
		context.write("indexed='1'")
	context.write(">%s - %s</option>\n" % (cls.code, cls.descrip))
%>
</select>
</td></tr>
<tr><th>Index</th> <td>
<select id='indexcode' name='indexcode'>
<option value=''></option>
%for code in sorted(c.classdata.indexlist):
	<option value='${code}'>${code}</option>
%endfor
</select>
</td></tr>
<tr><th>Number</th><td>
	<input id='number' name='number' type='hidden'/> 
	<span id='displaynumber'></span>
	<span id='numselector'><a id='availablelink' href='${h.url_for(action='available', code='XX')}' target='numberselection'>Select Number</a></span>
	</td></tr>
</tbody>
</table>
</form>

</%def>

