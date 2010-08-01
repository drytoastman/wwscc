
<style>
table.editor td { border: 1px solid #999; height: 20px; }
table.editor th { border: 1px solid #999; font-size: 10px; text-align: right; padding-right: 4px; width: 20px; }
table.editor { border-collapse: collapse; width: 500px;  }
button.editor { font-size: 11px !important; margin-bottom: 3px; }
button.ceditor { font-size: 9px !important; margin-bottom: 3px; }
div.editor { margin-left: 10px; margin-bottom: 15px; }
</style>

<div class='editor'>

<button class='editor' name='edit' driverid='${c.d.id}'>Edit</button>
<button class='editor' name='merge' driverid='${c.d.id}'>Merge Into This</button>
<button class='editor' name='delete' driverid='${c.d.id}'>Delete</button><br/>
<table class='editor'>
<tr><th>Id</th><td colspan='3'>${c.d.id}</td></tr>
<tr><th>Name</th><td colspan='3'>${c.d.firstname} ${c.d.lastname}</td></tr>
<tr><th>Email</th><td colspan='3'>${c.d.email}</td></tr>
<tr><th>Address</th><td colspan='3'>${c.d.address}</td></tr>
<tr><th>CSZ</th><td>${c.d.city}</td><td>${c.d.state}</td><td>${c.d.zip}</td></tr>
<tr><th>Clubs/Brag</th><td>${c.d.clubs}</td><td colspan='2'>${c.d.brag}</td></tr>
<tr><th>Sponsor/Membership</th><td>${c.d.sponsor}</td><td colspan='2'>${c.d.membership}</td></tr>

%for car in c.cars:
<tr><td colspan='4'>
<button class='ceditor' name='editcar' carid='${car.id}'>Edit</button>
<button class='ceditor' name='deletecar' carid='${car.id}'>Delete</button>
${car.classcode}(${car.indexcode}) #${car.number} ${car.year} ${car.make} ${car.model} ${car.color} 
</td></tr>
%endfor
</table>

</div>

<script>
$("button").button();
$("button").click(function() {
	alert("click " + $(this).attr("name"));
	if ($(this).attr('driverid')) {
		alert("driverid is " + $(this).attr('driverid'));
	} else if ($(this).attr('carid')) {
		alert("carid is " + $(this).attr('carid'));
	}
});
</script>

