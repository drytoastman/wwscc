
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

/* To Title Case 1.1.1
 * David Gouch <http://individed.com>
 * 23 May 2008
 * License: http://individed.com/code/to-title-case/license.txt
 *
 * In response to John Gruber's call for a Javascript version of his script: 
 * http://daringfireball.net/2008/05/title_case
 */

String.prototype.toTitleCase = function() {
    return this.replace(/([\w&`'".@:\/\{\(\[<>_]+-? *)/g, function(match, p1, index, title) {
        if (index > 0 && title.charAt(index - 2) !== ":" &&
        	match.search(/^(a(nd?|s|t)?|b(ut|y)|en|for|i[fn]|o[fnr]|t(he|o)|vs?\.?|via)[ \-]/i) > -1)
            return match.toLowerCase();
        if (title.substring(index - 1, index + 1).search(/['"_{(\[]/) > -1)
            return match.charAt(0) + match.charAt(1).toUpperCase() + match.substr(2);
        if (match.substr(1).search(/[A-Z]+|&|[\w]+[._][\w]+/) > -1 || 
        	title.substring(index - 1, index + 1).search(/[\])}]/) > -1)
            return match;
        return match.charAt(0).toUpperCase() + match.substr(1);
    });
};

