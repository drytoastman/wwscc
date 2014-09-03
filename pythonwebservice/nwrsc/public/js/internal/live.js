
function switchto(id)
{
    $('#main div').hide();
    $('#'+id).show();
}

function addWatcher(id, code, type)
{
    var nav = $("<li><a>"+id+"</a></li>");
    var div = $("<div>No Data</div>");

    nav.click(function() { switchto(id); });
    div.attr("id", id);
    div.data("code", code)
    div.data("type", type)
    div.data("updated", 0);

    $("#navcontainer ul").append(nav);
    $("#main").append(div);
}

function buildpage()
{
    $("#navcontainer").remove();
    $("#main div").remove();

    $("#header").append("<div id='navcontainer'><ul></ul></div>");

    var instructions = true;
    var includechamp = $("input[name='champon']").prop("checked");


    $("input:checked").each( function( ) {
        if (this.name == "champon") { return; }

        if (this.name == "pax") {
            addWatcher("PAX", "Any", "PAX");
        } else if (this.name == "raw") {
            addWatcher("Raw", "Any", "Raw");
        } else {
            addWatcher(this.name, this.name, "Event");
            if (includechamp) {
                addWatcher(this.name+"Champ", this.name, "Champ");
            }
        }

        instructions = false;
    });

    if (instructions) {
        $("#main").append("<div>Select classes to view from the menu in the top left.</div>");
        $("#navcontainer ul").append("<li>&nbsp</li>");
    }

    $("#navcontainer").navbar();
    $("#navcontainer a").first().click();

    lasttime = 0;
    updateCheck();
}


function processLast(json)
{
	$('#main div').each(function() {

		var me = $(this);
		var type = me.data('type');

		for (index in json)
		{
        	obj = json[index];
			if (obj.updated > lasttime)  // keep our last reference time up to date
				lasttime = obj.updated

			if ((obj.classcode == me.data('code')) && (obj.updated > me.data('updated')))
			{
				me.data('updated', obj.updated);
				me.load($.nwr.url_for(me.data('type'), obj.carid));
				break;
			}
		}
	});

	updateCheck();
}


function updateCheck()
{
	var codes = []
	$("#main div").each(function() {
		codes.push($(this).data('code'));
	});

	if (codes.length == 0) {
		return;
	}

	$.ajax({
			dataType: "json",
			url: $.nwr.url_for('last'),
			data: { time: lasttime, classcodes: codes.join() },
			success: processLast,
			error: function(xhr) { if (xhr.status != 403) { setTimeout('updateCheck()', 1000); } }
			});
}


var lasttime;
$(document).ready(function(){
	lasttime = 0;
    buildpage();

    $("#classpanel").panel({ close: function( event, ui ) {
        buildpage();
    } });
});


