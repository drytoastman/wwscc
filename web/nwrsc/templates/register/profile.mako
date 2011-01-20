<%inherit file="base.mako" />
<%namespace file="/forms/drivereditor.mako" import="drivereditor"/>
<%namespace file="/forms/driverform.mako" import="driverform"/>
<% from simplejson import dumps %>
<%def name="extrahead()">
<style type="text/css">
input.closebutton
{
   font-family:Verdana,sans-serif;
   font-weight:bold;
   font-size: 0.5em;
   color:#555;
   background-color:#FFF;
   border-width:1px;
   border-color: #555;
   width: 15px;
   height: 15px;
}
.labelcol .required 
{
	color: #800;
}
.ui-dialog {
    font-size: 0.75em !important;
}
</style>
</%def>

%if not c.driver:
<h2>Create Profile</h2>
<div class='infobox'>
<ul>
<li>First name, last name and email/uniqueid are required
</ul>
</div>

<% from nwrsc.model import Driver %>
${driverform(Driver(), h.url_for(action='newprofile'))}
<button onclick="$('#drivereditor').submit();">Create</button>

%else:

<h2>My Profile</h2>
<div class='display'>
<div>${c.driver.firstname} ${c.driver.lastname}</div>
<div>${c.driver.email}</div>
<br/>
<div>${h.hide(c.driver.address, 4)|n}</div>
<div>${c.driver.city} ${c.driver.state} ${c.driver.zip}</div>
<div>${h.hide(c.driver.homephone, 6)|n}</div>
<br/>
<div>Brag: ${c.driver.brag}</div>
<div>Sponsor: ${c.driver.sponsor}</div>
<div>Membership: ${c.driver.membership}</div>
</div>

<br/>

<button class='editor' onclick='editdriver(${c.driver.id});'>Edit</button>

<script>
var drivers = Array();
drivers[${c.driver.id}] = ${dumps(c.driver.__dict__, default=lambda x: str(x))|n} 

function driveredited()
{
	$('#drivereditor').attr('action', '${h.url_for(action='editprofile')}');
	$('#drivereditor').submit();
}

</script>
${drivereditor()}

%endif
