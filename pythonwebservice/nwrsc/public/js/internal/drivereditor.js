	
(function ($) {

	var methods = {

		doDialog: function(driver, okcallback) {
			$this = $(this);
			var title = driver.id && "Edit Driver" || "New Driver";
	
			$this.find('[name=driverid]').val(driver.id || -1);
			$this.find('[name=firstname]').val(driver.firstname || "");
			$this.find('[name=lastname]').val(driver.lastname || "");
			$this.find('[name=email]').val(driver.email || "");
			$this.find('[name=address]').val(driver.address || "");
			$this.find('[name=city]').val(driver.city || "");
			$this.find('[name=state]').val(driver.state || "");
			$this.find('[name=zip]').val(driver.zip || "");
			$this.find('[name=phone]').val(driver.phone || "");
			$this.find('[name=brag]').val(driver.brag || "");
			$this.find('[name=sponsor]').val(driver.sponsor || "");
			$this.find("[data-extrafield=true]").each(function () {
				var name = $(this).prop('name');
				$(this).val(driver[name] || "");
			});
			$this.find('[name=alias]').val(driver.alias || "");
			$this.find('.aliasspan').html(driver.alias || "");

			if (! $this.data('drivervalidate'))
			{
				$this.data('drivervalidate', true);
			    $this.validate({
					invalidHandler: function(e, validator) {
						var errors = validator.numberOfInvalids();
						if (errors) {
							$(".driverhelp").css('color', '#F00');
						} else {
							$(".driverhelp").css('color', '#999');
						}
					},
					errorPlacement: function(error, element) {
					},
					onkeyup: false,
					messages: {}
				});
			
			    $this.find('[name=firstname]').rules("add", {required:true, minlength:2});
			    $this.find('[name=lastname]').rules("add", {required:true, minlength:2});
			    $this.find('[name=email]').rules("add", {required:true, minlength:3});
			}
	
		
			$this.dialog({
				width: "auto",
				modal: true,
				title: title,
				buttons: {
					'Ok': function() {
						if ($(this).valid()) {
							$(this).dialog('close');
							okcallback();
						}
					},
					Cancel: function() {
						$(this).dialog('close');
						$(this).validate().resetForm();
					}
				},
				close: function() {
					$(this).find(".driverhelp").css('color', '#999');
				}
			});
		}
	};

	$.fn.DriverEdit = function( method ) {
		if ( methods[method] ) {
			return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else {
			$.error( 'Method ' +  method + ' does not exist on DriverEdit' );
		}    
	};

	
})(jQuery);


