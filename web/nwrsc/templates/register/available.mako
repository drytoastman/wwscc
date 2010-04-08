<%inherit file="/base.mako" />
<%def name="extrahead()">
<script type='text/javascript'>
function sn(num)
{
	opener.document.getElementById('number').value = num;
	opener.document.getElementById('displaynumber').innerHTML = num;
	window.close();
}
</script>

<style type='text/css'>
ul {
float: left;
margin: 0;
padding: 0;
list-style: none;
width: 400px;
}

li {
text-align: right;
font-size: 0.9em;
font-family: arial;
color: #EDD;
float: left;
width: 40px;
margin: 0;
padding: 0;
}

a {
text-decoration: none;
color: blue;
}
</style>
</%def>

<h3>Your Available Numbers For ${c.code} - Select One</h3>
<ul>
%for num in range(0, 2000):
%if num in c.numbers:
	<li>${num}</li>
%else:
	<li><a href='#' onclick='sn(${num});'>${num}</a></li>
%endif
%endfor
</ul>

