<%inherit file="base.mako" />
<%namespace file="/forms/careditor.mako" import="careditor"/>
<%namespace file="/forms/drivereditor.mako" import="drivereditor"/>
<%namespace file="events.mako" import="eventlist"/>
<%namespace file="cars.mako" import="carlist"/>

<script>
drivers = Array();
</script>

<style>

#seriesname {
	text-align: left;
}

#sponsor img {
	display: block;
	margin-left: 40px;
}

#content {
	min-width: 820px;
	width: auto !important;
	width: 820px;
}

#profile {
	margin: 1px;
	float: left;
	margin-right: 5px;
}

#events {
	float: left;
	margin: 1px;
	font-size: 0.7em;
	width: 300px;
	margin-right: 5px;
}

#drivereditor {
	font-size: 0.7em;
}


#cars {
	font-size: 0.7em;
	float: left;
	margin: 1px;
}

#carright {
	clear: both;
}

table.smalltable th { display: none; }
.cardrop {
	color: #aaa;
	text-align: center;
	border: 1px solid #aaa;
	width: 150px;
}

</style>

<div id='profile'>
<h3>Profile</h3>

<table class='smalltable'>
<tbody>
<tr><th>Id</th><td>${c.driver.id}</td></tr>
<tr><th>Name</th><td>${c.driver.firstname} ${c.driver.lastname}</td></tr>
<tr><th>Email</th><td>${c.driver.email}</td></tr>
<tr><th>Address</th><td>${c.driver.address}</td></tr>
<tr><th>CSZ</th><td>${c.driver.city}, ${c.driver.state} ${c.driver.zip}</td></tr>
<tr><th>Brag</th><td>${c.driver.brag}</td></tr>
<tr><th>Sponsor</th><td>${c.driver.sponsor}</td></tr>
%for field in c.fields:
<tr><th>${field.title}</th><td>${c.driver.getExtra(field.name)}</td></tr>
%endfor
</table>
<button class='editor' onclick='editdriver(${c.driver.id})'>Edit</button>
</div>

<div id='events'>
Events
${eventlist()}
</div>

<div id='cars'>
<h2>Cars</h2>
${carlist()}
</div>

<div id='carright'>
</div>

${drivereditor()}
${careditor()}
<script>
$(function() {
$("#events").accordion();
$("#editprofile").button();
})
</script>
