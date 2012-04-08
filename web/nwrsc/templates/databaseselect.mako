<%inherit file="base.mako" />
<h3>Select series:</h3>
<ol>
%for db in sorted(c.dblist, key=lambda x: x.name.lower()):
	<li><a href='${h.url_for(database=db.name)}'>${db.name}</a></li>
%endfor
</ol>
