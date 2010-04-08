<%inherit file="base.mako" />
<% from nwrsc.forms import personForm, personFormValidated %>
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
</style>
</%def>
<%def name="delete(val)">
<%doc>
<input type="button" name="${val}" class="closebutton" value="X" title="Delete ${val}"
	onMouseOver="this.style.backgroundColor='#FFF'; this.style.color='#B88';"
	onMouseOut="this.style.backgroundColor='#FFF'; this.style.color='#555';">
</%doc>
</%def>

%if not c.driver:
<h2>Create Profile</h2>
<div class='infobox'>
<ul>
<li>First name, last name and email are required
</ul>
</div>

${personFormValidated(action=h.url_for(action='newprofile'))|n}

%else:

<h2>My Profile</h2>
<div class='display'>
<div>${c.driver.firstname} ${c.driver.lastname}</div>
<div>${c.driver.email}</div>
<br/>
<div>${delete('address')} ${h.hide(c.driver.address, 4)|n}</div>
<div>${delete('czx')} ${c.driver.city} ${c.driver.state} ${c.driver.zip}</div>
<div>${delete('phone')} ${h.hide(c.driver.homephone, 6)|n}</div>
<br/>
<div>${delete('brag')} Brag: ${c.driver.brag}</div>
<div>${delete('sponsor')} Sponsor: ${c.driver.sponsor}</div>
<div>${delete('membership')} Membership: ${c.driver.membership}</div>
</div>

<h3>Update Data</h3>
<div class='infobox'>
<ul>
<li>To change information, enter any new information and click update.</li>
</ul>
</div>

${personForm(action=h.url_for(action='editprofile'))|n}

%endif
