<%def name="formatCar(car)">${("%s/%s - %s %s %s %s" % (car.classcode, car.number, car.year, car.make, car.model, car.color))[:37]}</%def>

<%def name="paypalLink(event)">
	<form class='paypalform' action='https://www.paypal.com/cgi-bin/webscr' method='post' target='_blank'>
	<span class='eimage'>
	<input type='hidden' name='cmd' value='_xclick' />
	<input type='hidden' name='business' value='${event.paypal}' />
	<input type='hidden' name='item_name' value='${event.name}' />
	<input type='hidden' name='custom' value='${event.id}.${c.driverid}' />
	<input type='hidden' name='amount' value='${event.cost}' />
	<input type='hidden' name='currency_code' value='USD' />
	<input type='hidden' name='notify_url' value='${h.url_for(action='ipn', protocol='http')}'>
	<input type='image' src='https://www.paypal.com/en_US/i/btn/x-click-but3.gif' name='submit'
						alt='Make payments with payPal - it&#039;s fast, free and secure!' />
	</span>
	</form>
</%def>


<%def name="carSelection(reg, event, cars, disabled)">
<div class='carselector'>
%if not reg:
	<select class='eselector' name='selectcarid' onchange='registerCar(this, ${event.id});'>
    <option selected='selected' value='-1'></option>
%else:
	<select class='eselector' name='selectcarid' onchange='reRegisterCar(this, ${event.id}, ${reg.id});'>
%endif


%for car in cars:
%if reg and car.id == reg.carid:
	<option selected='selected' value='${car.id}'>${formatCar(car)}</option>
%elif car.id not in disabled:
	<option value='${car.id}'>${formatCar(car)}</option>
%endif
%endfor
</select>

%if reg:
	<button onclick='unregisterCar(${event.id}, ${reg.id});'></button>
%endif
</div>
</%def>


<%def name="eventdisplay(ev)">
	<span class='elabel'>Closes:</span>
	<span class='evalue'>${ev.opened and ev.regclosed.strftime('%a %b %d at %I:%M%p') or 'Has not opened yet'}</span><br/>

	%if ev.host:
		<span class='elabel'>Host:</span>
		<span class='evalue'>${ev.host}</span><br/>
	%endif

	%if ev.location:
		<span class='elabel'>Location:</span>
		<span class='evalue'>${ev.location}</span><br/>
	%endif

	%if not ev.closed and ev.opened:
		%if ev.totlimit:
			<span class='elabel'>Limit:</span>
			<span class='evalue'>${ev.count}/${ev.totlimit}</span><br/>
		%endif
		%if ev.cost:
			<span class='elabel'>Cost:</span>
			<span class='evalue'>${ev.cost}</span><br/>
		%endif
		%if ev.paypal:
			<span class='elabel'>Paypal:</span>
			${paypalLink(ev)}
		%endif
		%if ev.snail:
			<span class='elabel'>Mail:</span>
			<div class='eaddress'>${ev.snail|n}</div>
		%endif
		%if ev.notes:
			<span class='elabel'>Notes:</span>
			<div class='enotes'>${ev.notes|n}</div>
		%endif
	%endif

	%if ev.closed or not ev.opened:
		%for reg in ev.regentries:
			${formatCar(reg.car)}<br/>
		%endfor
	%else:
		<%doc> Where they add a new registration </%doc>
		<div class='erule'>Register a Car</a></div>
		%if ev.totlimit and ev.count >= ev.totlimit:
			<span class='limit'>This event's prereg limit of ${ev.totlimit} has been met.</span>
		%elif len(ev.regentries) >= ev.perlimit:
			<span class='limit'>You have reached this event's prereg limit of ${ev.perlimit} car(s).</span>
		%else:
			${carSelection(None, ev, c.cars, [x.carid for x in ev.regentries])}
		%endif

		<%doc> Where they change their registration </%doc>
		<div class='erule'>Change/Unregister a Car</div>
		%for reg in ev.regentries:
			${carSelection(reg, ev, c.cars, [x.carid for x in ev.regentries])}
		%endfor

		<%doc> Space holder to keep accordian size at max requirement </%doc>
		%for ii in range(len(ev.regentries), ev.perlimit):
			<div class='carspaceholder'></div>
		%endfor

		<%doc> Just information payment information </%doc>
		%if len(ev.payments) > 0:
			<div class='erule'>Paypal Payments</div>
			%for p in ev.payments:
				${p.amount} (${p.status})<br>
			%endfor
		%endif
	%endif

	<script>
	$(".carselector button").button({icons: { primary:'ui-icon-closethick'}, text: false} );
	</script>

</%def>

