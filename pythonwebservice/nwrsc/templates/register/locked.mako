<%inherit file="base.mako" />
<%namespace file="genericview.mako" import="viewlist"/>

<div id='lockedmessage' class='ui-state-error'>
<span class='ui-state-error-text'>
The database is currently locked for an event or administration, no changes can be made at
this point. Please try again in a day or two after the event.
</span>
</div>

${viewlist()}

