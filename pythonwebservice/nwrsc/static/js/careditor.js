(function ($) {

	var methods = {

        currentclass: function() {
			return gClasses[$(this).find('[name=classcode] option:selected').val()];
        },

		classchange: function() {
            var myform = this;
			var cc = methods.currentclass.call(myform);
			var indexselect = myform.find('[name=indexcode]');
            
			if (cc.isindexed) {
				indexselect.toggle(true);
			    myform.find('[name=indexcode]').parent().toggle(true);
				var restrict = cc.idxrestrict;
                if (restrict.length == 0) {
                    restrict = gIndexes;
                }
				
				indexselect.find("option").remove();
                indexselect.append(new Option("", "", false, false));
   				for (var ii = 0; ii < restrict.length; ii++) {
                    indexselect.append(new Option(restrict[ii], restrict[ii], false, false));
                }
			} else {
				indexselect.val(0);
			    myform.find('[name=indexcode]').parent().toggle(false);
			}
		
            // Clear indexcode and number, they are invalid now
			methods.indexchange.call(myform);
            myform.find('[name=number]').val("");

            // Get the objects we want and set initial values for the structure
            var code = myform.find('[name=classcode] option:selected').val();
            var numobj = myform.find('[name=number]').data('usednumbers', []);
            $("a[href=\"#usedlist\"] span.label").text(" Unavailable Numbers in " + code);
            var ul = $("#usedlist ul").html("loading...");

            // Actually make a request for the list and update the UL
            $.get("usednumbers", {'classcode': code}, function (data) {
                numobj.data('usednumbers', data);
                ul.empty();
                $.each(data, function(ii, num) {
                    ul.append("<li>"+num+"</li>");
                });
            });
		},
	

		indexchange: function() {
            var myform = this;
			var tireindexcontainer = myform.find('[name=tireindexed]').parent();
			var cc = methods.currentclass.call(myform);
			var ci = myform.find('[name=indexcode] option:selected').val() || "noindex";
			var restrict = cc.flagrestrict;

			if (cc.usecarflag && ((restrict.length == 0) || ($.inArray(ci, restrict) >= 0))) {
				tireindexcontainer.toggle(true)
			} else {
				myform.find('[name=tireindexed]').prop('checked', false);
				tireindexcontainer.toggle(false)
			}
		},


		initform: function(car) {
            var myform = this;
			if (!myform.data('cardialoginit'))
			{
				myform.data('cardialoginit', true);
				myform.find('[name=classcode]').change(function() { methods.classchange.call(myform); });
				myform.find('[name=indexcode]').change(function() { methods.indexchange.call(myform); });
                myform.find('[name=number]').after("<div style='margin:auto;width:100%'><a data-toggle='collapse' href='#usedlist'><span class='fa'/><span class='label'>Numbers Used By Others</span></a><div id='usedlist' class='collapse'><ul></ul></div></div>");
                add_collapse_icons('#usedlist');

				$.validator.setDefaults({ignore:[]});
				myform.validate({
					rules: {
						indexcode: {
							required: function(element) { return (methods.currentclass.call(myform).isindexed); }
						},
						number: {
							required: true,
                            digits: true,
                            notinused: true,
							min: 1,
							max: 1999
						}
					},
			
					messages: { 
						indexcode: 'an index is required for indexed classes',
					},
				});
			}

			myform.find('[name=carid]').val(car.carid || -1);
			myform.find('[name=year]').val(car.year || "");
			myform.find('[name=make]').val(car.make || "");
			myform.find('[name=model]').val(car.model || "");
			myform.find('[name=color]').val(car.color || "");
			myform.find('[name=classcode]').val(car.classcode || "");
			methods.classchange.call(myform);
			myform.find('[name=indexcode]').val(car.indexcode || "");
			methods.indexchange.call(myform);
			myform.find('[name=tireindexed]').prop('checked', car.tireindexed);
            myform.find('[name=number]').val(car.number || "");

		}
	};


	$.fn.CarEdit = function( method ) {
		if ( methods[method] ) {
			return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else {
			$.error( 'Method ' +  method + ' does not exist on CarEdit' );
		}    
	};

}) (jQuery);


$.validator.addMethod("notinused", function( value, element ) {
    var used = $(element).data("usednumbers") || [];
    return ($.inArray(parseInt(value), used) < 0);
},  "that number is already in use");

