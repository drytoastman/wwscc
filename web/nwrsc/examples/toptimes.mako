<%def name="toptimestable(toptimes)">

<style>
.numcol { border-left: 1px solid #BBB; text-align: right; }
.brgrey { border-right: 1px solid #BBB }
</style>

<table class='toptimestable'>
<tr>
%for toptimelist in toptimes:
<th class='classhead' colspan='${len(toptimelist.cols)+1}'>${toptimelist.title}</th>
%endfor
</tr>

<tr>
%for toptimelist in toptimes:
<th>#</th>
%for col in toptimelist.cols:
<th>${col}</th>
%endfor
%endfor
</tr>

%for ii, fullrow in enumerate(zip(*[x.rows for x in toptimes])):
<tr class='brgrey'>
%for listrow in fullrow:
<td class='numcol'>${ii+1}</td>
%for item in listrow:
<td>${item}</td>
%endfor
%endfor
</tr>
%endfor
</table>
</%def>

<%inherit file="/base.mako" />
${toptimestable(c.toptimes)}

