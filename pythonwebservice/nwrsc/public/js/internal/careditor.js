(function ($) {

	var methods = {

		classchange: function() {

			var myform = this;
			var indexcontainer = myform.find('.indexcodecontainer');
			var tireindexcontainer = myform.find('.tireindexcontainer');

			if (myform.find('[name=classcode] option:selected').data('indexed')) {
				indexcontainer.toggle(true);
			} else {
				myform.find('[name=indexcode]').val(0);
				indexcontainer.toggle(false);
			}
		
			
			if (myform.find('[name=classcode] option:selected').data('usecarflag')) {
				tireindexcontainer.toggle(true)
			} else {
				tireindexcontainer.toggle(false)
			}
		
			methods.setnum.call(myform, "");
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
				position: [20, 100],
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
			myform.find('[name=carid]').val(car.id || "");
			myform.find('[name=year]').val(car.year || "");
			myform.find('[name=make]').val(car.make || "");
			myform.find('[name=model]').val(car.model || "");
			myform.find('[name=color]').val(car.color || "");
			myform.find('[name=classcode]').val(car.classcode || "");
			myform.find('[name=indexcode]').val(car.indexcode || "");
			myform.find('[name=tireindexed]').prop('checked', car.tireindexed);
			methods.classchange.call(myform);
			methods.setnum.call(myform, car.number || "");


			if (! myform.data('cardialoginit'))
			{
				myform.data('cardialoginit', true);
				myform.find('.numberselect').button().click(function() { methods.chooseNumber.call(myform); return false; });
				myform.find('[name=classcode]').change(function() { methods.classchange.call(myform); });

				myform.validate({
					rules: {
						indexcode: {
							required: function(element) { return $("[name=classcode] option:selected").data('indexed'); }
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
				position: [20, 100],
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

