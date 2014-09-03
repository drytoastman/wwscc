<%inherit file="base.mako" />

<div data-role="page" data-theme="b">

	<div data-role="header">
		<h1>Select Series</h2>
	</div> 

	<div data-role="content">
%for db in sorted(c.dblist, key=lambda x:x.name):
	<a href='${h.url_for(database=db.name)}/' data-role='button' rel="external">${db.name}</a>
%endfor
	</div> 

</div>
	

