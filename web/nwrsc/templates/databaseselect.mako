<%inherit file="base.mako" />

<h3>Select series:</h3>
<ol>
%for file in sorted(c.files):
	<li><a href='${h.url_for(database=file[:-3])}'>${file[:-3]}</a></li>
%endfor
</ol>
