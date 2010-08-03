
<style>
table.editor td { border: 1px solid #999; height: 20px; }
table.editor th { border: 1px solid #999; font-size: 10px; text-align: right; padding-right: 4px; width: 20px;}
table.editor { border-collapse: collapse; width: 600px;  }
button.editor { font-size: 11px !important; margin-bottom: 3px; }
button.ceditor { font-size: 9px !important; margin-bottom: 3px; margin-top: 2px; }
div.editor { margin-left: 10px; margin-bottom: 15px; }
</style>


<% allids = [x.driver.id for x in c.items] %>

%for info in c.items:

<% anyruns = sum([x.runs for x in info.cars]) %>

<div class='editor'>

<button class='editor' onclick='editdriver(${info.driver.id});'>Edit</button>
%if len(c.items) > 1:
<button class='editor' onclick='mergedriver(${info.driver.id}, ${allids});'>Merge Into This</button>
%endif
<button class='editor' onclick='deletedriver(${info.driver.id});' ${anyruns and "disabled='disabled'"}>Delete</button><br/>
<table class='editor'>
<tbody>
<tr><th>Id</th><td>${info.driver.id}</td></tr>
<tr><th>Name</th><td>${info.driver.firstname} ${info.driver.lastname}</td></tr>
<tr><th>Email</th><td>${info.driver.email}</td></tr>
<tr><th>Address</th><td>${info.driver.address}</td></tr>
<tr><th>CSZ</th><td>${info.driver.city}, ${info.driver.state} ${info.driver.zip}</td></tr>
<tr><th>Clubs</th><td>${info.driver.clubs}</td></tr>
<tr><th>Brag</th><td>${info.driver.brag}</td></tr>
<tr><th>Sponsor</th><td>${info.driver.sponsor}</td></tr>
<tr><th>Membership</th><td>${info.driver.membership}</td></tr>

%for car in info.cars:
<tr>
<td colspan='2'>
<button class='ceditor' onclick='editcar(${car.id});'>Edit</button>
<button class='ceditor' onclick='deletecar(${car.id});' ${car.runs and "disabled='disabled'"}>Delete</button>
${car.classcode}(${car.indexcode}) #${car.number} ${car.year} ${car.make} ${car.model} ${car.color} (${car.runs} events)
</td>
</tr>
%endfor
</tbody>
</table>

</div>

%endfor

<script>
$("button").button();
</script>

