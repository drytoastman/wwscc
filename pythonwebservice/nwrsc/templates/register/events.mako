<%namespace file="displays.mako" import="carDisplay"/>

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



<%def name="eventdisplay(ev)">

    <div class='detailscontainer'>
	<table class='eventdetails'>
		<tr><th>Closes</th><td>${ev.opened and ev.regclosed.strftime('%a %b %d at %I:%M%p') or 'Has not opened yet'}</td></tr>
	%if ev.host:
		<tr><th>Host</th><td>${ev.host}</td></tr>
	%endif
	%if ev.location:
		<tr><th>Location</th><td>${ev.location}</td></tr>
	%endif
	%if ev.opened:
		<tr><th>Entries</th><td>
		<a class='viewlink' href='${h.url_for(action='view', other=ev.id)}' >
		%if ev.totlimit:
			${ev.count}/${ev.totlimit}
		%else:
			${ev.count}
		%endif
		</a></td></tr>
	%endif

	%if ev.isOpen: 
		%if ev.cost:
			<tr><th>Cost</th><td>$${ev.cost}</td></tr>
		%endif
		%if ev.paypal:
			<tr><th>Paypal</th><td>${paypalLink(ev)}</td></tr>
		%endif
		%if ev.notes:
			<tr><th>Notes</th><td>${ev.notes|n}</td></tr>
		%endif
	%endif
	</table>
	</div>

	<div class='carcontainer'>
		<ul>
		%for reg in ev.regentries:
			<li>
			%if ev.isOpen:
			<button class='unregbutton' data-eventid='${ev.id}' data-regid='${reg.id}'>Unregister</button>
			%endif
			${carDisplay(reg.car)}
			</li>
		%endfor
		</ul>

		%if ev.isOpen:
			%if ev.totlimit and ev.count >= ev.totlimit:
				<span class='limit'>This event's prereg limit of ${ev.totlimit} has been met.</span>
			%elif len(ev.regentries) >= ev.perlimit:
				<span class='limit'>You have reached this event's prereg limit of ${ev.perlimit} car(s).</span>
			%else:
				<button class='regcarsbutton' data-eventid="${ev.id}">Register Cars</button>
			%endif
	
			<%doc> Just information payment information </%doc>
			%if len(ev.payments) > 0:
				<ul class='payments'>
				%for p in ev.payments:
					<li>${p.amount} (${p.status})</li>
				%endfor
				</ul>
			%endif
		%endif

	</div>

</%def>

