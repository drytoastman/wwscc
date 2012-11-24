<ul class='numbers'>
%for num in range(0, c.largest+1):
%if num in c.used:
	<li>${num}</li>
%else:
	<li><a href='#' onclick='setnum(${num});'>${num}</a></li>
%endif
%endfor
</ul>
