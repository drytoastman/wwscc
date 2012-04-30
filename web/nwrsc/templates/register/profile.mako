<%def name="profile()">

<script>
drivers = Array();
drivers[${c.driver.id}] = ${h.encodesqlobj(c.driver)|n}
</script>

<span class='name'>
${c.driver.firstname} ${c.driver.lastname}
%if c.driver.alias:
(${c.driver.alias})
%endif
</span>
<span class='email'>${c.driver.email}</span>
<span class='address'>${c.driver.address}</span>
<span class='csz'>${c.driver.city} ${c.driver.state} ${c.driver.zip}</span>
<span class='brag'>${c.driver.brag}</span>
<span class='dsponsor'>${c.driver.sponsor}</span>
%for field in c.fields:
<span class='e-${field.name}'>${field.name}: ${c.driver.getExtra(field.name)}</span>
%endfor

</%def>
