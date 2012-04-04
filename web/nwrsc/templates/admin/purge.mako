<%inherit file="base.mako" />
<% from operator import attrgetter %>

<style>
table.purge ul { margin: 0; padding: 0; list-style: none; }
table.purge td { vertical-align: top; padding: 13px; }
table.purge { border-collapse: collapse; }
</style>

<h2>Purge Tool</h2>

<p>
This tool will purge unused drivers and cars from the current database by looking back at previous series to see if they have been active.  The previous series selected should be parent series to this series or car and driver ids will not match properly.  If the car or driver has runs recoreded in any of the previous selected series, they will remain present, otherwise they will be removed from the current series.  Note, this is only intended to be done after creating a new database, not part way through a series.  If no series are selected, the purge tool will keep all drivers and rely only on the class list for purging.
</p>

<p>
You can also request that cars from the selected classes are purged regardless of their previous activity, such as Time Only classes.  Simply select the classes to purge and submit.  If series are only selected, the two purging processes are combined, previous activity and then class purge.
</p>

<p class='ui-state-error-text'>
NOTE: Purge Classes removes EVERYONE from that class.  This is intended for time only classes.
</p>

<form action='${h.url_for(action='processPurge')}' method='post'>
<table class='purge'>
<tr><th>Search Series</th><th colspan='${int(len(c.classlist)/20)+1}'>Purge Classes</th></tr><tr>

<td>
%for f in sorted(c.files, key=str.lower):
<input type='checkbox' name='s-${f}'/> ${f[:-3]}<br/>
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
<input type='submit' value='Submit'/>
</td>

</tr></table>
</form>
