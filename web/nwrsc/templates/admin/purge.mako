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
This tool will remove cars and or drivers that haven't been active.  You can select series or classes.
</p>

<p>
For each series you select, the list of active cars and drivers will be
extended to include any activity in those series.  The previous series selected
should be parent series to this series or car and driver ids will not match
properly.  
</p>

<p>
For any class that you select, <span class='ui-state-error-text'>all of the car
entries are removed</span> except for those active in the current series.  This
is intended for major cleaning such as time only classes.
</p>

<form action='${h.url_for(action='processPurge')}' method='post'>
<table class='purge'>
<tr><th>Search Series</th><th colspan='${int(len(c.classlist)/20)+1}'>Purge Classes</th></tr><tr>

<td>
%for f in sorted(c.files, key=lambda x:x.name.lower()):
<input type='checkbox' name='s-${f.name}'/> ${f.name}<br/>
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
