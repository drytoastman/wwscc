
(function( $ ){

    var methods = {

        loadEvent: function(eventid, callback) {  // need to use .get as load will try and use POST with args
            var me = this;
            $.get($.nwr.url_for('getevent'), {eventid:eventid}, function(data) { me.html(data); callback.call(me); });
			return me;
        },

        loadCars: function() {
            return this.load($.nwr.url_for('getcars'));
        },

        loadProfile: function() {
            return this.load($.nwr.url_for('getprofile'));
        },

        loadNumbers: function(classcode, driverid, callback) {
            return this.load($.nwr.url_for('carnumbers'), {code:classcode, driverid:driverid}, callback);
        }

    };


    $.fn.nwr = function( method ) {
        if ( methods[method] ) {
            return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else {
            $.error( 'Method ' +  method + ' does not exist on nwr' );
        }
    };


    $.nwr = {
		deleteCar: function(carid, callback) { $.post($.nwr.url_for(action='deletecar'), {carid:carid}, callback); },
        newDriver: function(args, callback) { $.post($.nwr.url_for('newdriver'), args, callback); },
        unRegisterCar: function(regid, callback) { $.post($.nwr.url_for('unRegisterCar'), {regid:regid}, callback); },
        updateDriver: function(args, callback) { $.post($.nwr.url_for('editdriver'), args, callback); },
        updateCar: function(args, callback) { $.post($.nwr.url_for('editcar'), args, callback); },

        url_for: function(action) { return window.location.href.substring(0, window.location.href.lastIndexOf('/')+1) + action }
    };


})( jQuery );


