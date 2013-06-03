(function ($) {

	var methods = {

		classchange: function() {

			var myform = this;
			var indexcontainer = myform.find('.indexcodecontainer');
			var currentclass = myform.find('[name=classcode] option:selected');
			var indexselect = myform.find('[name=indexcode]');

			if (currentclass.data('indexed')) {
				indexcontainer.toggle(true);
				var restrict = currentclass.data('idxrestrict') || "";
				restrict = restrict.split(',')
				
				indexselect.find("option").remove();
				indexselect.append(new Option("", "", false, false));
				for (var ii = 0; ii < gIndexList.length; ii++) {
					if ($.inArray(gIndexList[ii], restrict) < 0) {
						indexselect.append(new Option(gIndexList[ii], gIndexList[ii], false, false));
					}
				}

			} else {
				indexselect.val(0);
				indexcontainer.toggle(false);
			}
		
			methods.indexchange.call(myform);
			methods.setnum.call(myform, "");
		},
	

		indexchange: function() {
			// Need separate function to capture this when only index changes

			var myform = this;
			var tireindexcontainer = myform.find('.tireindexcontainer');
			var currentclass = myform.find('[name=classcode] option:selected');
			var currentindex = myform.find('[name=indexcode] option:selected').val() || "noindex";
			var restrict = currentclass.data('flagrestrict') || "";
			restrict = restrict.split(',')

			if (currentclass.data('usecarflag') && ($.inArray(currentindex, restrict) < 0)) {
				tireindexcontainer.toggle(true)
			} else {
				myform.find('[name=tireindexed]').prop('checked', false);
				tireindexcontainer.toggle(false)
			}
		
		},


		setnum: function(val) {

			var myform = this;
			input = myform.find('[name=number]');
			input.val(val);
			if (input.attr('type') == 'hidden') {
				myform.find('.numberdisplay').html(val);
				if (val != "")
					myform.validate().form();
			}
			
		},


		chooseNumber: function() {
			var mainForm = this;
			var numberForm = $('#numberselection');
			if (!numberForm) {
				numberForm = $('<div/>', {id:'numberselection'}).appendTo('body');
			}

			var classcode = mainForm.find('[name=classcode] option:selected').val();
			var driverid = mainForm.find('[name=driverid]').val();

			// set html to loading, open the dialog, load the available numbers and then set click behaviour
			numberForm.html('loading ...').dialog({
				height: 400,
				width: 480,
				//position: [20, 100],
				modal: true,
				title: 'Available Numbers',
				close: function() {}
			}).nwr('loadNumbers', classcode, driverid, function() {
				// when load is complete,  update all the links to call setnum
				numberForm.find('a').click(function() { 
					methods.setnum.call(mainForm, $(this).data('carnum')); 
					numberForm.dialog('close');
					return false;
				});
			});
		},


		doDialog: function(driverid, car, okcallback) {

			var myform = this;

			myform.find('[name=driverid]').val(driverid);
			myform.find('[name=carid]').val(car.id || -1);
			myform.find('[name=year]').val(car.year || "");
			myform.find('[name=make]').val(car.make || "");
			myform.find('[name=model]').val(car.model || "");
			myform.find('[name=color]').val(car.color || "");
			myform.find('[name=classcode]').val(car.classcode || "");
			methods.classchange.call(myform);
			myform.find('[name=indexcode]').val(car.indexcode || "");
			methods.indexchange.call(myform);
			myform.find('[name=tireindexed]').prop('checked', car.tireindexed);
			methods.setnum.call(myform, car.number || "");


			if (! myform.data('cardialoginit'))
			{
				myform.data('cardialoginit', true);
				myform.find('.numberselect').button().click(function() { $(this).blur(); methods.chooseNumber.call(myform); return false; });
				myform.find('[name=classcode]').change(function() { methods.classchange.call(myform); });
				myform.find('[name=indexcode]').change(function() { methods.indexchange.call(myform); });

				$.validator.setDefaults({ignore:[]});
				myform.validate({
					rules: {
						indexcode: {
							required: function(element) { return (myform.find("[name=classcode] option:selected").data('indexed') == true); }
						},
						number: {
							required: true,
							min: 1,
							max: 1999
						}
					},
			
					messages: { 
						indexcode: 'an index is required for indexed classes',
						number: 'a number is required'
					},
			
					invalidHandler: function(e, validator) {
						var errors = validator.numberOfInvalids();
						if (errors) {
							$(".carerror").html("");
							$(".carerror").show();
						} else {
							$(".carerror").hide();
						}
					},
			
					errorPlacement: function(error, element) {
						$(".carerror").append(error);
					}
				});
			}
		
		
			myform.dialog({
				width: "auto",
				modal: true,
				title: 'Car Editor',
				//position: [20, 100],
				buttons: {
					'Ok': function() {
						if (myform.valid()) {
							myform.dialog('close');
							okcallback();
						}
					},
					Cancel: function() { myform.dialog('close'); }
				},
				close: function() {
				}
			});

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

