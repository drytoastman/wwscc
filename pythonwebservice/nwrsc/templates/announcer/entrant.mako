<%namespace file="runs.mako" import="runslist"/>
<%namespace file="class.mako" import="classlist"/>
<%namespace file="champ.mako" import="champlist"/>

${runslist()}

<table style='width:100%; margin-top:5px;'><tr><td width=50%>

${classlist()}

</td><td width=50%>

${champlist()}

</td></tr></table>




