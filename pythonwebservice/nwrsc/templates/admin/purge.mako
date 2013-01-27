<%inherit file="base.mako" />
<% from operator import attrgetter %>

<style>
table.purge ul { margin: 0; padding: 0; list-style: none; }
table.purge td { vertical-align: top; padding: 13px; }
table.purge { border-collapse: collapse; }
p { max-width: 700px; }
</style>

<h2>Purge Tool</h2>

<p>
This tool will remove cars and or drivers that haven't been active.  You can select series and/or classes.
Regardless of selection, drivers and cars active in this series will not be touched.
</p>

<p>
For each series you select, the list of active cars and drivers will be
extended to include any activity in those series.  If no series are selected, no overall purging will take place.
</p>

<p>
For any class that you select, <span class='ui-state-error'>all of the car
entries are removed</span>, EXCEPT for those active in the current series.  This
is intended for major cleaning such as time only classes, generally at the
beginning of the year.
</p>

<p>
The last option is to delete all the drivers (and associated cars) that have blank email/unique ids
as they are unable to preregister online anyhow.
</p>

<form action='${h.url_for(action='processPurge')}' method='post'>
<table class='purge'>
<tr><th>Keep Activity From Series</th><th colspan='${int(len(c.classlist)/20)+1}'>Purge Classes</th><th>Onsite Drivers</tr><tr>

<td>
%for dbname in c.lineage: 
<input type='checkbox' name='s-${dbname}'/> ${dbname}<br/>
%endfor
</td>

<td>
<ul>
%for ii, cls in enumerate(sorted(c.classlist, key=attrgetter('code'))):
<li><input type='checkbox' name='c-${cls.code}'/> ${cls.code}</li>
%if ((ii+1) % 20) == 0:
</ul></td><td><ul>
%endif
%endfor
</ul>
</td>

<td>
<input id='onsitedrivers' type='checkbox' name='onsitedrivers'/>
<label for='onsitedrivers' style='display:inline-block; width:120px;'>Delete onsite drivers and cars</label>
</td>

<td>
<input type='submit' value='Submit'/>
</td>

</tr></table>
</form>
