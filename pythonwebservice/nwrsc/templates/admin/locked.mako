<%inherit file="base.mako" />
<div style='margin-top:30px;margin-right:30px;color:red;font-weight:bold;'>
The database is currently locked for an event or administration.
</div>
%if c.isAdmin:
<div style='margin-top: 10px; margin-left: 10px;'>
<input type='button' value='Force Unlock' onclick='

if (confirm("Unlocking a database that could be checked in again later is a bad opertion, are you sure?")) {
	window.location = "${h.url_for(action='forceunlock', next=c.next)}";
}

'/>
</div>
%endif
