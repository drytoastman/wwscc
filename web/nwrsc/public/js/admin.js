
function switchdisp(hideDIV, showDIV)
{
	document.getElementById(hideDIV).style.display = "none";
	document.getElementById(showDIV).style.display = "block";
}

function clearsearch(inputname, bodyname)
{
	var input = document.getElementById(inputname);
	input.value = '';
	tablesearch(inputname, bodyname);
}

function Person(id, fn, ln, em)
{
	this.personid = id;
	this.firstname = fn;
	this.lastname = ln;
	this.email = em;
}

function clearNavigator(inputname)
{
	var input = document.getElementById(inputname);
	input.value = '';
	searchNavigator(inputname);
}

function searchNavigator(inputname)
{
	var input = document.getElementById(inputname);
	var searchstr = new RegExp(input.value, "i");
	var matches = new Array();
	for (var ii = 0; ii < personData.length; ii++)
	{
		if (personData[ii].firstname.match(searchstr) ||
			personData[ii].lastname.match(searchstr) ||
			personData[ii].email.match(searchstr))
			matches.push(personData[ii]);
	}

	var display = document.getElementById("showithere");
	if (matches.length < 10)
	{
		var result = '<ol>';
		for (var ii = 0; ii < matches.length; ii++)
		{
			var url = "/people/"+matches[ii].personid;
			var script = 'window.opener.document.location="'+url+'"; window.opener.focus();';
			result += "<li><a href='javascript:void(0);' onClick='"+script+"'>";
			result += matches[ii].firstname + " " + matches[ii].lastname + " (" + matches[ii].email + ")</a></li>";
		}
		result += '</ol>';
	}
	else
	{
		var result = "There are " + matches.length + " matches<br>";
	}
	display.innerHTML = result;
}

