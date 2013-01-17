
<%!
	def rnl(text):
		return text.replace('\n', '')
%>

<%def name="carDisplay(car)" filter="rnl" >
<div class='cardisplay'>
<span class='code'>${car.classcode}/${car.number}</span>
<span class='desc'>${car.year} ${car.make} ${car.model} ${car.color} ${h.ixstr(car)}</span> 
</div>
</%def>

