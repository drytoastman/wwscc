

function switchto(newid)
{
    $('#'+current).css('display', 'none');
    $('#'+newid).css('display', 'block');
    current = newid;
}

function processLast(json)
{
    for (index in json)
    {
        obj = json[index];
        match = views[obj.classcode]
		if (obj.updated > lasttime)
			lasttime = obj.updated
        if (obj.updated >  match.updated)
        {
            match.updated = obj.updated;
            for (index1 in match.pages)
            {
                var divid = match.pages[index1]; 
				var type = $('#'+divid).data('type');
				$('#'+divid).load($.nwr.url_for(type, obj.carid));
            }
        }
    }
	updateCheck();
}

function updateCheck()
{
	var codes = $.map(views, function(v, k) { return k; });
	$.ajax({
			dataType: "json",
			url: $.nwr.url_for('last'),
			data: { time: lasttime, classcodes: codes.join() },
			success: processLast,
			error: function(xhr) { if (xhr.status != 403) { setTimeout('updateCheck()', 3000); } }
			});
}


var current = "ignoremenotpresent";
var views = {};

$(document).ready(function(){

	$(".viewer").each(function() {
		var me = $(this);
		var code = me.data('code');

		if (!(code in views)) {
			views[code] = { updated: 0, pages: [] };
		}

		views[code].pages.push(me.attr('id'));
	});

	lasttime = 0;
    updateCheck();
    $('#navbar a').first().click();
});


