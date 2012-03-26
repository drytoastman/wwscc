<%def name="carform(numberhidden)">

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

#numberdisplay {
margin-right: 10px;
font-size: 1.1em;
color: #000;
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
<select id='classcode' name='classcode'>
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

