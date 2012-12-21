<%def name="carform(numberhidden)">

<form id='careditor' action='' method='post'>
<div id='carerror' class='ui-state-error-text'></div>
<input id='driverid' name='driverid' type='hidden'/>
<input id='carid' name='carid' type='hidden'/>
<table class='careditor'>
<tbody>
<tr><th>Year</th>  <td><input name='year'   type='text'/></td></tr>
<tr><th>Make</th>  <td><input name='make'   type='text'/></td></tr>
<tr><th>Model</th> <td><input name='model'  type='text'/></td></tr>
<tr><th>Color</th> <td><input name='color'  type='text'/></td></tr>
<tr><th>Class</th> <td>
<select id='classcode' name='classcode'>
<%
for code in sorted(c.classdata.classlist):
	cls = c.classdata.classlist[code]
	context.write("<option value='%s' " % (cls.code))
	if cls.carindexed:
		context.write("indexed='1' ")
	if cls.usecarflag:
		context.write("usecarflag='1' ")
	context.write(">%s - %s</option>\n" % (cls.code, cls.descrip))
%>
</select>
</td></tr>

<tr class='indexcodecontainer'><th>Index</th> <td>
<select id='indexcode' name='indexcode'>
<option value=''></option>
%for code in sorted(c.classdata.indexlist):
	<option value='${code}'>${code}</option>
%endfor
</select>
</td></tr>

<tr class='tireindexcontainer'><th>Tire Index</th> <td> <input name='tireindexed' type='checkbox'/></td></tr>

<tr>
   <th>Number</th><td>
	%if numberhidden:
		<input name='number' type='hidden'/>
		<span id='numberdisplay'></span>
	%else:
	   <input name='number' type='text' size='3' />
	%endif
   <button id="numberselect">Available</button>
   </td>
</tr>
</tbody>
</table>
</form>


<div id='numberselection'>
</div>

</%def>

