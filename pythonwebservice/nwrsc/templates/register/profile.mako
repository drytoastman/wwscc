<%def name="profile()">

<span class='name'>
${c.driver.firstname} ${c.driver.lastname}
%if c.driver.alias:
(${c.driver.alias})
%endif
</span>
<span class='email'>${c.driver.email}</span>
<span class='address'>${c.driver.address}</span>
<span class='csz'>${c.driver.city} ${c.driver.state} ${c.driver.zip}</span>
<span class='phone'>${c.driver.phone}</span>
%if c.driver.brag:
<span class='brag'>Brag: ${c.driver.brag}</span>
%endif
%if c.driver.sponsor:
<span class='dsponsor'>Sponsor: ${c.driver.sponsor}</span>
%endif
%for field in c.fields:
<span class='e-${field.name}'>${field.name}: ${c.driver.getExtra(field.name)}</span>
%endfor

<a class='registeredlink' href='${h.url_for("registerlink", first=c.driver.firstname, last=c.driver.lastname, email=c.driver.email)}'>
<span class='ui-icon ui-icon-calculator'></span>Registered Cars iCal Link
</a> 

<input  type='button' value='Edit' class='editprofile' data-driverid='${c.driver.id}'/> 

<script type='text/javascript'>
var drivers = { ${c.driver.id}: ${h.encodesqlobj(c.driver)|n} }
profileTabSetup();
</script>

</%def>
