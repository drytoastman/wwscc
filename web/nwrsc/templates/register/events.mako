<%inherit file="base.mako" />

<%def name="formatCar(car)">${car.classcode}/${car.number} - ${car.year} ${car.make} ${car.model} ${car.color}</%def>

<%def name="paypalLink(event)">
	<form style='display:inline;' action='https://www.paypal.com/cgi-bin/webscr' method='post' target='_blank'>
	<span class='eimage'>
	<input type='hidden' name='cmd' value='_xclick' />
	<input type='hidden' name='business' value='${event.paypal}' />
	<input type='hidden' name='item_name' value='${event.name}' />
	<input type='hidden' name='custom' value='${event.id}.${c.driverid}' />
	<input type='hidden' name='amount' value='${event.cost}' />
	<input type='hidden' name='currency_code' value='USD' />
	<input type='hidden' name='notify_url' value='${h.url_for(action='ipn')}'>
	<input type='image' src='https://www.paypal.com/en_US/i/btn/x-click-but3.gif' name='submit'
						alt='Make payments with payPal - it&#039;s fast, free and secure!' />
	</span>
	</form>
</%def>


<%def name="carSelection(reg, event, cars, disabled)">
<form action='${h.url_for(action='register')}' method='post'>
<div>
<input type='hidden' name='carid' value='0' />
<input type='hidden' name='eventid' value='${event.id}' />
<input type='hidden' name='regid' value='${reg and reg.id or 0}' />
<select class='eselector' name='selectcarid' onchange='submitAndWait(this, "selectcarid");'>
%if reg:
	<option value='-1'>--- unregister this entry ---</option>
%else:
	<option selected='selected' value='-1'></option>
%endif

%for car in cars:
%if reg and car.id == reg.carid:
	<option selected='selected' value='${car.id}'>${formatCar(car)}</option>
%elif car.id not in disabled:
	<option value='${car.id}'>${formatCar(car)}</option>
%endif
%endfor
</select>
</div>
</form>
</%def>


<h2>Series Events</h2>
<div class='infobox'>
<ul>
<li>To register a car for an event, select a car from the drop down menu</li>
<li>To change/unregister a car, select a different car or delete from the appropriate drop down menu</li>
<li>To create a new car, edit or delete cars, click the <a href='${h.url_for(action='cars')}'>My Cars</a> tab to the left</li>
</ul>
</div>
<table class='eventlist'>
<tr>
<th>Event</th>
<th>Registration</th>
</tr>

<%doc> <li>If the event is already full, you will be placed on a wait list</li> <%doc>
<input type='button' onclick='$("#eventrow1,#eventrow2,#eventrow3,#eventrow4").css("display", "none");' value='hide'>
<input type='button' onclick='$("#eventrow1,#eventrow2,#eventrow3,#eventrow4").css("display", "");' value='show'>
</%doc>

%for ev in sorted(c.events, key=lambda obj: obj.date):
	<%doc> Event Info cell </%doc>
	<tr id='eventrow${ev.id}'>
	<td class='${ev.tdclass}' valign='top'>
		<a class='elink' href='${h.url_for(action='view', event=ev.id)}'>${ev.name}</a>
		<span class='elabel'>Date:</span>
		<span class='evalue'>${ev.date.strftime('%a %b %d')}</span><br/>
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
			<div class='eaddress'>${ev.snail}</div>
		%endif
		%if ev.notes:
			<span class='elabel'>Notes:</span>
			<div class='enotes'>${ev.notes}</div>
		%endif
	%endif

	</td>
	<td class='${ev.tdclass}'>

	<%doc> Car selection cell  </%doc>
	%if ev.closed or not ev.opened:
		%for reg in ev.regentries:
			${formatCar(reg.car)}<br/>
		%endfor
	%else:
		<%doc> Where they add a new registration </%doc>
		<div class='erule'>Register a car from <a href='${h.url_for(action='cars')}'>My Cars</a></div>
		%if ev.totlimit and ev.count >= ev.totlimit:
			<span class='limit'>This event's prereg limit of ${ev.totlimit} has been met.</span>
		%elif len(ev.regentries) >= ev.perlimit:
			<span class='limit'>You have reached this event's prereg limit of ${ev.perlimit} car(s).</span>
		%else:
			${carSelection(None, ev, c.cars, ev.regentries)}
		%endif

		<%doc> Where they change their registration </%doc>
		%if len(ev.regentries) > 0:
			<div class='espacer'></div>
			<div class='erule'>Change/Unregister a currently registered car</div>
			%for reg in ev.regentries:
				${carSelection(reg, ev, c.cars, ev.regentries)}
			%endfor
		%endif

		<%doc> Just information payment information </%doc>
		%if len(ev.payments) > 0:
			<div class='espacer'></div>
			<div class='erule'>Paypal Payments</div>
			%for p in ev.payments:
				${p.amount} (${p.status})<br>
			%endfor
		%endif
	%endif
	</td>
	</tr>

%endfor
</table>

