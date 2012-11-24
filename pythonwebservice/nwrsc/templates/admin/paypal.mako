<%inherit file="/base.mako" />
<h2>Paypal Payments for ${c.event.name}</h2>
<table>
<tr>
<th>TxId</th>
<th>Name</th>
<th>Value</th>
<th>Type</th>
<th>Status</th>
</tr>
%for p in c.payments:
<tr>
<td>${p.txid}</td>
<td>${p.driver.lastname}, ${p.driver.firstname}</td>
<td>${p.amount}</td>
<td>${p.type}</td>
<td>${p.status}</td>
</tr>
%endfor
</table>
