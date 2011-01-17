<%inherit file="base.mako" />

<h2>Purge Tool</h2>

<p>
This tool will purge unused drivers and cars from the current database by looking back at previous series to see if they have been active.  The previous series selected should be parent series to this series or car and driver ids will not match properly.  If the car or driver has runs recoreded in any of the previous selected series, they will remain present, otherwise they will be removed from the current series.  Note, this is only intended to be done after creating a new database, not part way through a series.
</p>

<h3>Purge Using</h3>

<form action='${h.url_for(action='processPurge')}' method='post'>
%for f in c.files:
<input type='checkbox' name='${f}'/> ${f}<br/>
%endfor
<br/>
<input type='submit' value='Submit'/>
</form>
