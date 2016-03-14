<%def name="carform(numberhidden)">

<form id='careditor' method='post' class='ui-helper-hidden'>
<div class='carerror ui-state-error-text'></div>
<input name='driverid' type='hidden'/>
<input name='carid' type='hidden'/>
<table class='careditor'>
<tbody>
<tr><th>Year</th>  <td><input name='year'   type='text'/></td></tr>
<tr><th>Make</th>  <td><input name='make'   type='text'/></td></tr>
<tr><th>Model</th> <td><input name='model'  type='text'/></td></tr>
<tr><th>Color</th> <td><input name='color'  type='text'/></td></tr>
<tr><th>Class</th> <td>
<select name='classcode'>
<%
for code in sorted(c.classdata.classlist):
	cls = c.classdata.classlist[code]
	restrict = cls.restrictedIndexes()
	context.write("<option value='%s' " % (cls.code))
	if cls.carindexed:
		context.write("data-indexed='true' ")
	if len(restrict[0]) > 0:
		context.write("data-idxrestrict='%s' " %  ','.join(restrict[0]))
	if len(restrict[1]) > 0:
		context.write("data-flagrestrict='%s' " %  ','.join(restrict[1]))
	if cls.usecarflag:
		context.write("data-usecarflag='true' ")
	context.write(">%s - %s</option>\n" % (cls.code, cls.descrip))
%>
</select>
</td></tr>
<tr><th></th><td colspan="2">
%if c.settings.classinglink:
<a target='classinghelp' href='${c.settings.classinglink}'>Click Here for Classing Help</a>
%endif
</td></tr>

<script>
gIndexList = [ "${'","'.join(sorted(c.classdata.indexlist)) |n}" ];
</script>

<tr class='indexcodecontainer'><th>Index</th> <td>
<select name='indexcode'>
<option value=''></option>
</select>
</td></tr>

<tr class='tireindexcontainer'><th>Tire Index</th> <td> <input name='tireindexed' type='checkbox'/></td></tr>

<tr>
   <th>Number</th><td>
	%if numberhidden:
		<input name='number' type='hidden'/>
		<span class='numberdisplay'></span>
	%else:
	   <input name='number' type='text' size='3' />
	%endif
   <button class="numberselect">Available</button>
   </td>
</tr>
</tbody>
</table>
</form>


<div id='numberselection'>
</div>

</%def>

