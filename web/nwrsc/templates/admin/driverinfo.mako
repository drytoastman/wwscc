<%
allids = [x.driver.id for x in c.items]
%>

%for info in c.items:

<script>
drivers[${info.driver.id}] = ${h.encodesqlobj(info.driver)|n}
</script>

<% anyruns = sum([x.runs for x in info.cars]) %>

<div class='editor' id='driver${info.driver.id}'>
<button class='editor' onclick='editdriver(${info.driver.id});'>Edit</button>
%if len(c.items) > 1:
<button class='editor' onclick='mergedriver(${info.driver.id}, ${allids});'>Merge Into This</button>
%endif
<button class='editor' onclick='deletedriver(${info.driver.id});' ${anyruns and "disabled='disabled'"}>Delete</button>
<button class='editor' onclick='titlecasedriver(${info.driver.id});'>TitleCase</button><br/>


<table class='editor'>
<tbody>
<tr><th>Id</th><td>${info.driver.id}</td></tr>
<tr><th>Name</th><td>${info.driver.firstname} ${info.driver.lastname}</td></tr>
<tr><th>Alias</th><td>${info.driver.alias}</td></tr>
<tr><th>Email</th><td>${info.driver.email}</td></tr>

<tr><th>Address</th><td>${info.driver.address}</td></tr>
<tr><th>CSZ</th><td>${info.driver.city}, ${info.driver.state} ${info.driver.zip}</td></tr>
<tr><th>Brag</th><td>${info.driver.brag}</td></tr>
<tr><th>Sponsor</th><td>${info.driver.sponsor}</td></tr>

%for field in c.fields:
<tr><th>${field.title}</th><td>${info.driver.getExtra(field.name)}</td></tr>
%endfor

%for car in info.cars:
<script>
cars[${car.id}] = ${h.encodesqlobj(car)|n} 
</script>
<tr>
<td class='carcell' colspan='2'>
<button class='ceditor' onclick='editcar(${info.driver.id}, ${car.id});'>Edit</button>
<button class='ceditor' onclick='deletecar(${car.id});' ${car.runs and "disabled='disabled'"}>Delete</button>
<button class='ceditor' onclick='titlecasecar(${car.id});'>TitleCase</button>
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

