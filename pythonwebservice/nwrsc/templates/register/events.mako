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


<%def name="carSelection(event, cars)">
<div class='carselector'>
<select class='eselector' name='selectcarid' onchange='registerCar(this, ${event.id});'>
    <option selected='selected' value='-1'></option>
	%for car in cars:
	<option value='${car.id}'>${formatCar(car)}</option>
	%endfor
</select>
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
			<tr>
			<th>Cost</th>
			<td>$${ev.cost}</td>
			</tr>
		%endif
		%if ev.paypal:
			<tr>
			<th>Paypal</th>
			<td class='evalue'>${paypalLink(ev)}</td>
			</tr>
		%endif
		%if ev.notes:
			<tr>
			<th>Notes</th>
			<td>${ev.notes|n}</td>
			</tr>
		%endif
	%endif

	<tr>
	<th>Registered Cars</th>
	<td>
	%for reg in ev.regentries:
		<div>
		%if not ev.closed:
		<button class='unregbutton' data-eventid='${ev.id}' data-regid='${reg.id}'>Unregister</button>
		%endif
		${carDisplay(reg.car)}
		</div>
	%endfor

	%if ev.opened and not ev.closed:
		<!--<div class='erule'>Register a Car</div>-->
		<%doc> Where they add a new registration </%doc>
		%if ev.totlimit and ev.count >= ev.totlimit:
			<span class='limit'>This event's prereg limit of ${ev.totlimit} has been met.</span>
		%elif len(ev.regentries) >= ev.perlimit:
			<span class='limit'>You have reached this event's prereg limit of ${ev.perlimit} car(s).</span>
		%elif len(c.cars) == 0:
			<span class='notifier'>You need to <a class='cartablink'>Create New Car</a> in the Cars Tab</span>
		%else:
			${carSelection(ev, c.cars)}
		%endif

		<%doc> Just information payment information </%doc>
		%if len(ev.payments) > 0:
			<div class='erule'>Paypal Payments</div>
			%for p in ev.payments:
				${p.amount} (${p.status})
			%endfor
		%endif
	%endif
	</td>
	</tr>
	</table>

</%def>

