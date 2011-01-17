<%def name="careditor()">

<script>
function classchange()
{
	if ($("#classcode option:selected").attr('indexed'))
	{
		$('#indexcode').removeAttr('disabled');
	}
	else
	{
		$('#indexcode').val(0);
		$('#indexcode').attr('disabled', 'disabled');
	}

}

function editcar(did, cid)
{
	$('#careditor [name=driverid]').val(did);
	$('#careditor [name=carid]').val(cid);
	$('#careditor [name=year]').val(cars[cid].year);
	$('#careditor [name=make]').val(cars[cid].make);
	$('#careditor [name=model]').val(cars[cid].model);
	$('#careditor [name=color]').val(cars[cid].color);
	$('#careditor [name=classcode]').val(cars[cid].classcode);
	$('#careditor [name=indexcode]').val(cars[cid].indexcode);
	classchange();
	$('#careditor [name=number]').val(cars[cid].number);
	$('#careditor').dialog('open');
	// open editor
}

function selectnumber()
{
	$('#numberselection').html("loading...");
	$('#numberselection').dialog('open');
	$('#numberselection').load('${h.url_for(action='carnumbers')}', {
				code : $("#careditor #classcode option:selected").val(),
				driverid : $('#careditor [name=driverid]').val()
			});
}

function setnum(v)
{
	$('#careditor [name=number]').val(v);
	$('#numberselection').dialog('close');
}

$(document).ready(function(){
    $("#careditor").validate({
		rules: {
			indexcode: {
				required: function(element) { return ($("#classcode option:selected").attr('indexed') == '1'); },
			},
			'number': {
				required: true,
				min: 1,
				max: 1999,
			}
		},
		messages: { 
			indexcode: 'an index is required for indexed classes'
		},
	});

	$("#careditor").dialog({
		autoOpen: false,
		height: 320,
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

	$("#numberselection").dialog({
		autoOpen: false,
		height: 400,
		width: 480,
		modal: true,
		title: 'Available Numbers',
		close: function() {}
	});
});
</script>

<style type='text/css'>
ul.numbers {
float: left;
margin: 0;
padding: 0;
list-style: none;
width: 410px;
}

ul.numbers li {
text-align: right;
font-size: 1.1em;
font-family: arial;
color: #EDD;
float: left;
width: 40px;
margin: 0;
padding: 0;
}

ul.numbers a {
text-decoration: none;
color: blue;
}
</style>

<form id='careditor'>
<input id='driverid' name='driverid' type='hidden'/>
<input id='carid' name='carid' type='hidden'/>
<table class='careditor'>
<tbody>
<tr><th>Year</th>  <td><input name='year'   type='text'/></td></tr>
<tr><th>Make</th>  <td><input name='make'   type='text'/></td></tr>
<tr><th>Model</th> <td><input name='model'  type='text'/></td></tr>
<tr><th>Color</th> <td><input name='color'  type='text'/></td></tr>
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
<tr>
   <th>Number</th><td>
   <input name='number' type='text' size='1' readonly/>
   <button onclick='selectnumber();' style='font-size:0.8em;'>Available</button>
   </td>
</tr>
</tbody>
</table>
</form>


<div id='numberselection'>
</div>


</%def>

