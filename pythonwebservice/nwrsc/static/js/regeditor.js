
(function ($) {

	var methods = {

		initform: function(eventid, limit) {
			var me = $(this);
            var checkmax = function() {
				var dodisable = (me.find(":checked").length >= limit);
				me.find('input[type=checkbox]:unchecked').prop('disabled', dodisable);
				me.find('.statuslabel').html( dodisable && "Entry Limit Reached" || ""); 
			};

			me.find('input[name=eventid]').val(eventid);
			me.find('input[type=checkbox]').prop('checked', false).prop('disabled', false).click(checkmax);
			gRegistered[eventid].forEach(function (carid) {
                me.find("input[name="+carid+"]").prop('checked', true);
			});
            checkmax();
		}
	};


	$.fn.RegEdit = function( method ) {
		if ( methods[method] ) {
			return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else {
			$.error( 'Method ' +  method + ' does not exist on RegEdit' );
		}    
	};

}) (jQuery);


