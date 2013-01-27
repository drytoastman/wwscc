
var saveids = Array();
var cars = Array();
var drivers = Array();

function newCountedRow(listform, listtemplate)
{
	$('.deleterow').click(function() { $(this).closest('tr').remove(); return false; });
	var ii = $(listform + ' tr:last').data('counter') + 1;
    var rowelem = $(listtemplate + ' tr').clone();
	rowelem.data('counter', ii);
    rowelem.find("input").attr("name", function(i, val) { return val.replace(/xxxxx/g, ii); });
    rowelem.find(".deleterow").click(function () { rowelem.remove(); return false; });
    rowelem.appendTo(listform + ' tbody');
    return false;
}

function buildselect(json)
{
	var select = $('#driverlist');
	if (select.prop) {
		var options = select.prop('options');
	} else {
		var options = select.attr('options');
	}

	$('option', select).remove();

	for (idx in json.data)
	{
		var dr = json.data[idx]
		options[idx] = new Option(dr[1] + " " + dr[2], dr[0]);
	}

	if (saveids.length > 0) {
		select.val(saveids);
		select.change();
	} else {
		$("#driverinfo").html("");
	}

	filterlist();
}


function filterlist()
{
	var v = $('#driverregex').val().toLowerCase();

	$("#driverlist option").each( function()
	{
		if ((v == '') || ($(this).text().toLowerCase().indexOf(v) >= 0)) {
			$(this).show();
		} else {
			$(this).hide();
		}
	});
}


function editdriver(did)
{
	$('#drivereditor').DriverEdit("doDialog", drivers[did], function() {
		$.nwr.updateDriver($("#drivereditor").serialize(), function() { 
			$("#driverlist").change();
		})
	});
}

function editcar(did, cid)
{
	$('#careditor').CarEdit('doDialog', did, cars[cid], function() {
		$.nwr.updateCar($("#careditor").serialize(), function() {
			$("#driverlist").change();
		})
	});
}

function deletedriver(did)
{
	$.post($.nwr.url_for('deletedriver'), { driverid: did }, function() {
		// Note ids to save and then rebuild driverlist and reselect, slow but always a sure sync with database
		saveids = $('#driverlist').val();
		for (var idx in saveids)
		{
			if (saveids[idx] == did)
			{
				saveids.splice(idx, 1);
				break;
			}
		}
		$.getJSON($.nwr.url_for('getdrivers'), {}, buildselect);
	});
}

function deletecar(cid)
{
	$.post($.nwr.url_for('deletecar'), { carid: cid }, function() {
		$("#driverlist").change(); // force reload of driver info
	});
}

function mergedriver(did, allids)
{
	$.post($.nwr.url_for('mergedriver'), { driverid: did, allids: allids.join(',') }, function() {
		saveids = [""+did];
		$.getJSON($.nwr.url_for('getdrivers'), {}, buildselect);
	});
}

function titlecasedriver(did)
{
	$.post($.nwr.url_for('titlecasedriver'), { driverid: did }, function() {
		$('option', $('#driverlist')).remove(); // fix for IE bug
		saveids = [""+did];
		$.getJSON($.nwr.url_for('getdrivers'), {}, buildselect);
	});
}

function titlecasecar(cid)
{
	$.post($.nwr.url_for('titlecasecar'), { carid: cid }, function() {
		$("#driverlist").change(); // force reload of driver info
	});
}

function downloadfull()
{
    rows = $('#contacttable').dataTable()._('tr', {"filter":"applied"});
    ids = Array();
    for (var ii = 0; ii < rows.length; ii++) {
        ids.push(rows[ii][0]);
    }
    $("input[name=ids]").attr('value', ids);
}

function copyemail()
{
    rows = $('#contacttable').dataTable()._('tr', {"filter":"applied"});
    email = Array();
    for (var ii = 0; ii < rows.length; ii++) {
        if (rows[ii][3].indexOf("@") > 1)  // dumb but still useful filter
            email.push(rows[ii][3]);
    }
    prompt("Copy the follow string to your clipboard", email);
    return false;
}

function collectgroups(frm)
{
    for (var ii = 0; ii < 3; ii++)
    {
        var x = Array();
        $("#group"+ii+" li").each(function() {
            x.push(this.innerHTML);
        });
        frm['group'+ii].value = ""+x;
    }
}


$(window).load(function(){
    // wait until image loads before updating text info for series settings
    $(".imageinfo").each(function() {
        var img = $(this).siblings('img');
        if (img.width() > 0) {
            $(this).text(img.width() + "w x " + img.height() + "h");
        }
    });
});


$(document).ready(function(){
	$.ajaxSetup({ cache: false });
	$("ul.sf-menu").supersubs({
		minWidth:    10,
		maxWidth:    100,
		extraWidth:  5
	}).superfish({
		disableHI: true,
		animation:   {height:'show'},
		speed: 'fast',
		autoArrows: false,
		delay: 200
	}).width('100%');

	$(':submit').button();
});
		
