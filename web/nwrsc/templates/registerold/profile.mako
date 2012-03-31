<%inherit file="base.mako" />
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
.error-message { color: red; }
.labelcol .required { color: #800; }
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
${driverform(action=h.url_for(action='newprofile'))}
<button onclick="$('#drivereditor').submit();">Create</button>

%else:

<h2>My Profile</h2>

<% from nwrsc.model import Driver %>
${driverform(action=h.url_for(action='editprofile'), driver=c.driver)}
<button onclick="$('#drivereditor').submit();">Update</button>
<script>
$('input').keydown( function() { $(this).css('background', 'yellow')} );
</script>

%endif
