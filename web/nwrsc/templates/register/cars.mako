<%inherit file="base.mako" />

<%def name="carArgs(car)">'${car.id}', '${car.year}', '${car.make}', '${car.model}', '${car.color}', '${car.classcode}', '${car.indexcode}', '${car.number}'</%def>

<h2>My Cars</h2>
<div class='infobox'>
<ul>
<li>To register one of these cars for an event, use the <a href='${h.url_for(action='events')}'>Events</a> tab to the left</li>
</ul>
</div>

<form name='carform' id='carform' action='${h.url_for(action='editcar')}' method='post'>
<div id='carsdisplay' style='display:block;'>
<table class='carlist'>
<tbody>

<tr><th colspan='4'>Cars In Use</th></tr>
<tr><td class='info' colspan='4'>Cars in use can only have their description changed</td></tr>
%for car in c.inuse:
	<tr>
	<td>${car.year} ${car.make} ${car.model} ${car.color}</td>
	<td>${car.classcode} ${car.indexcode and "(%s)" % car.indexcode}</td>
	<td>${car.number}</td>
	<td><input type='button' value='update description' onclick="update(${carArgs(car)});" /></td>
	</tr>
%endfor

<tr style='border-left:0px !important;'><td colspan='4' style='border-left:0px !important;border-right:0px;'></td></tr>
<tr><th colspan='4'>Unused Cars</th></tr>
<tr><td class='info' colspan='4'>Cars not in use can be fully modified or deleted</td></tr>
%for car in c.notinuse:
	<tr>
	<td>${car.year} ${car.make} ${car.model} ${car.color}</td>
	<td>${car.classcode} ${car.indexcode and "(%s)" % car.indexcode}</td>
	<td>${car.number}</td>
	<td>
		<input type='button' value='modify' onclick="modify(${carArgs(car)});" />
		<input type='button' value='delete' onclick="del('carform', ${carArgs(car)});" />
	</td>
	</tr>
%endfor

</tbody>
</table>
<input class='addnew' type='button' name='create' value='Create New Car' onclick='newcar();'/>
</div>

<div id='careditor' style='display:none;'>
<input type='hidden' id='carid' name='carid' value=''/>
<input type='hidden' id='ctype' name='ctype' value=''/>
<table class='careditor'>
<thead>
<tr><th colspan='2'>Car Editor</th></tr>
</thead>

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
<option></option>
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

<br/>
<input type='submit' id='submitbutton' onclick='return checkRegForm();' value='OK' />&nbsp;
<input type='button' value='Cancel' onclick='switchtocars();' />
<br/>
</div>
</form>

