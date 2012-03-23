<%def name="profile()">

<script>
drivers = Array();
drivers[${c.driver.id}] = ${h.encodesqlobj(c.driver)|n}
</script>

<span id='driverid'>${c.driver.id}</span>
<span id='name'>${c.driver.firstname} ${c.driver.lastname}</span>
<span id='email'>${c.driver.email}</span>
<span id='address'>${c.driver.address}</span>
<span id='csz'>${c.driver.city}, ${c.driver.state} ${c.driver.zip}</span>
<span id='brag'>${c.driver.brag}</span>
<span id='sponsor'>${c.driver.sponsor}</span>
%for field in c.fields:
<span id='${field.name}'>{c.driver.getExtra(field.name)}</span>
%endfor

</%def>
