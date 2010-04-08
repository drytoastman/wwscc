
function checkform()
{
	msg = "";
	if (document.loginform.firstname.value == '')
		msg += "missing firstname\n"; 

	if (document.loginform.lastname.value == '')
		msg += "missing lastname\n"; 

	if (document.loginform.email.value == '') 
		msg += "missing email\n";

	if (msg == '')
		return true;
	alert(msg);
	return false;
}

function notechange(fld)
{
    fld.style.backgroundColor = '#eeee22';
}

function submitAndWait(item, disableName)
{
	item.parentNode.carid.value = item.value;
	list = document.getElementsByName(disableName);
	for (ii = 0; ii < list.length; ii++)
	{
		list[ii].disabled = true;
	}
	item.parentNode.submit();
}

function switchtoeditor()
{
	document.getElementById("carsdisplay").style.display = "none";
	document.getElementById("careditor").style.display = "block";
}

function switchtocars()
{
	document.getElementById("carsdisplay").style.display = "block";
	document.getElementById("careditor").style.display = "none";
}

// Can't assign to .value in IE
function setSelectValue(select, value)
{
	select.selectedIndex = -1;
	for (ii = 0; ii < select.length; ii++)
	{
		if (select.options[ii].text == value)
		{
			select.selectedIndex = ii;
			break;
		}
	}
}

function setvalues(id, year, make, model, color, classcode, indexcode, num)
{
	document.getElementById("carid").value = id;
	document.getElementById("year").value = year;
	document.getElementById("make").value = make;
	document.getElementById("model").value = model;
	document.getElementById("color").value = color;
	setSelectValue(document.getElementById("classcode"), classcode);
	setSelectValue(document.getElementById("indexcode"), indexcode);
	document.getElementById("number").value = num;
	document.getElementById("displaynumber").innerHTML = num;
}

function setdisables(which, bool)
{
	if (which == "descrip")
	{
		document.getElementById("year").disabled = bool;
		document.getElementById("make").disabled = bool;
		document.getElementById("model").disabled = bool;
		document.getElementById("color").disabled = bool;
	}
	else if (which == "class")
	{
		//document.getElementById("number").disabled = bool;
		document.getElementById("classcode").disabled = bool;

		// Make sure index is enabled/disabled appropriatrly 
		if (bool == true)
		{
			document.getElementById("numselector").style.display = 'none';
			document.getElementById("indexcode").disabled = true;
		}
		else
		{
			document.getElementById("numselector").style.display = 'inline';
			num = document.getElementById("number").value;  // save number in this case
			classchange(); // depends on class
			document.getElementById("number").value = num;
			document.getElementById("displaynumber").innerHTML = num;
		}
	}
}

function modify(id, year, make, model, color, classcode, indexcode, num)
{
	setvalues(id, year, make, model, color, classcode, indexcode, num);
	setdisables("descrip", false);
	setdisables("class", false);
	document.getElementById("ctype").value = "modify";
	document.getElementById("submitbutton").value = "Modify";
	switchtoeditor();
}

function update(id, year, make, model, color, classcode, indexcode, num)
{
	setvalues(id, year, make, model, color, classcode, indexcode, num);
	setdisables("descrip", false);
	setdisables("class", true);
	document.getElementById("ctype").value = "update";
	document.getElementById("submitbutton").value = "Update";
	switchtoeditor();
}

function del(formid, carid, year, make, model, color, classcode, indexcode, num)
{
	if (confirm("Are you sure you wish to delete \"#" + num + " " + year + " "+ make + " " + model + " - " + classcode + "\"?"))
	{
		document.getElementById("carid").value = carid;
		document.getElementById("ctype").value = "delete";
		document.getElementById(formid).submit();
	}
}
	
function newcar()
{
	setvalues(-1, "", "", "", "", "", "", "");
	setdisables("descrip", false);
	setdisables("class", false);
	document.getElementById("ctype").value = "new";
	document.getElementById("submitbutton").value = "Create";
	switchtoeditor();
}

function classchange()
{
	box = document.getElementById("classcode");
	indexbox = document.getElementById("indexcode");
	document.getElementById("number").value = '';
	document.getElementById("displaynumber").innerHTML = '';

	index = box.selectedIndex;
	if (index < 0) {
		isindexed = false;
	} else {
		isindexed = box.options[index].getAttribute('indexed')
	}

	if (isindexed) {
		indexbox.disabled = false;
	} else {
		indexbox.disabled = true;
		indexbox.selectedIndex = -1;
	}

	adjustAvailableLink();
}

function adjustAvailableLink()
{
	link = document.getElementById("availablelink");
	box = document.getElementById("classcode");

	index = box.selectedIndex;
	if (index < 0) {
		link.href = "available/";
	} else {
		link.href = "available/" + box.options[index].text;
	}
}

function checkRegForm()
{
	if (document.getElementById("number").value == '')
	{
		alert("You need a car number.");
		return false;
	}

	box = document.getElementById("classcode");
	clsidx = box.selectedIndex;
	if (clsidx < 0)
	{
		alert("You need a class. (How did you get here?)");
		return false;
	}

	if (box.options[clsidx].getAttribute('indexed') &&
			(document.getElementById("indexcode").selectedIndex <= 0)) // 0 is the no index selection
	{
		alert("You need an index for " + box.options[clsidx].text);
		return false;
	}

	return true;
}

