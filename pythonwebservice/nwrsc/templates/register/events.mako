<%namespace file="displays.mako" import="carDisplay"/>

<%def name="formatCar(car)">
${"%s %s #%s - %s %s %s %s" % (car.classcode, h.ixstr(car), car.number, car.year, car.make, car.model, car.color)}
</%def>

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
	<button onclick='unregisterCar(this, ${event.id}, ${reg.id});'></button>
%endif
</div>
</%def>


<%def name="eventdisplay(ev)">

	<table class='eventdetails'>
		<tr>
		<th>Closes</th>
		<td>${ev.opened and ev.regclosed.strftime('%a %b %d at %I:%M%p') or 'Has not opened yet'}</td>
		</tr>
	%if ev.host:
		<tr>
		<th>Host</th>
		<td>${ev.host}</td>
		</tr>
	%endif
	%if ev.location:
		<tr>
		<th>Location</th>
		<td>${ev.location}</td>
		</tr>
	%endif
	%if ev.opened:
		<tr>
		<th>Entries</th>
		<td>
		%if not ev.closed:
		<a class='viewlink' href='${h.url_for(action='view', event=ev.id)}' >
		%endif
		%if ev.totlimit:
		${ev.count}/${ev.totlimit}
		%else:
		${ev.count}
		%endif
		%if not ev.closed:
		</a>
		%endif
		</td>
		</tr>
	%endif

	%if not ev.closed and ev.opened:
		%if ev.cost:
			<span class='elabel'>Cost:</span>
			<span class='evalue'>$${ev.cost}</span>
		%endif
		%if ev.paypal:
			<span class='elabel'>Paypal:</span>
			<span class='evalue'>${paypalLink(ev)}</span>
		%endif
		%if ev.snail:
			<span class='elabel'>Mail:</span>
			<div class='evalue eaddress'>${ev.snail|n}</div>
		%endif
		%if ev.notes:
			<span class='elabel'>Notes:</span>
			<div class='evalue enotes'>${ev.notes|n}</div>
		%endif
	%endif
		<tr>
		<th>Cars</th><td>
	%for reg in ev.regentries:
		<div>
		%if not ev.closed:
		<button class='unregbutton' onclick='unregisterCar(this, ${ev.id}, ${reg.id});'>Unregister</button>
		%endif
		${carDisplay(reg.car)}
		</div>
	%endfor
		<td></tr>
	</table>


	%if ev.opened and not ev.closed:
		<%doc> Where they add a new registration </%doc>
		<div class='erule'>Register a Car</div>
		%if ev.totlimit and ev.count >= ev.totlimit:
			<span class='limit'>This event's prereg limit of ${ev.totlimit} has been met.</span>
		%elif len(ev.regentries) >= ev.perlimit:
			<span class='limit'>You have reached this event's prereg limit of ${ev.perlimit} car(s).</span>
		%elif len(c.cars) == 0:
			<span class='notifier'>You need to "Create New Car" on the right</span>
		%else:
			${carSelection(None, ev, c.cars, [x.carid for x in ev.regentries])}
		%endif

		<%doc> Just information payment information </%doc>
		%if len(ev.payments) > 0:
			<div class='erule'>Paypal Payments</div>
			%for p in ev.payments:
				${p.amount} (${p.status})
			%endfor
		%endif
	%endif

	<script type='text/javascript'>
	$(".unregbutton").button({icons: { primary:'ui-icon-scissors'}, text: false} );
	</script>

</%def>

