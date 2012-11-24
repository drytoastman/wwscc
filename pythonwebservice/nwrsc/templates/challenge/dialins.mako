<%inherit file="/base.mako" />
<%def name="extrahead()">
<style type='text/css'>
td { border-top: 1px dashed black; padding: 2px 10px 1px 10px; }
td.sort { font-weight: bold; }
h2 { margin-bottom: 0px; }
h4 { margin-top: 0px; }
</style>
</%def>

<h2>${c.event.name} - Dialins</h2>
<h4>${c.filter} - Ordered by ${c.order}</h4>

<table class='dialins sortable'>
<tr>
<th class='sorttable_nosort'>Pos</th>
<th>Name</th>
<th>Class</th>
<th>Index</th>
<th>Value</th>
<th>Net</th>
<th>ClsDiff</th>
<th>Bonus</th>
<th>Regular</th>
</tr>

%for ii, e in enumerate(c.dialins):
	<tr>
	<td>${ii+1}</td>
	<td>${e.firstname} ${e.lastname}</td>
	<td>${e.classcode}</td>
	<td>${e.indexStr}</td>
	<td>${"%0.3f"%e.indexVal}</td>
	%if c.order == 'Net':
	<td class="sort">${"%0.3f"%e.net}</td>
	<td>${"%0.3f"%e.diff}</td>
	%else:
	<td>${"%0.3f"%e.net}</td>
	<td class="sort">${"%0.3f"%e.diff}</td>
	%endif
	<td>${"%0.3f"%e.bonusDial}</td>
	<td>${"%0.3f"%e.classDial}</td>
	</tr>
%endfor
</table>


