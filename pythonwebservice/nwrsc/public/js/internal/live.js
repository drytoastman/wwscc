

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
    $.getJSON($.nwr.url_for('last'), { classcodes: codes.join() }, processLast);
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

    updateCheck();
    $('#navbar a').first().click();
});


