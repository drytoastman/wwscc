<%inherit file="/base.mako" />

<table class='layout' id='mainlayout'><tr><td>

<div id='lasttimebox' class='ui-widget'>
<span class='header'>Timer</span>
<span id='timeroutput'>0.000</span>
</div>

<div id='runorder' class='ui-widget'></div>

<div id="entranttabs">
    <ul>
        <li><a href="#nexte"><span>Next to Finish</span></a></li>
        <li><a href="#firste"><span>Last to Finish</span></a></li>
        <li><a href="#seconde"><span>Second to Last</span></a></li>
    </ul>
    <div id="nexte" updated='0'></div>
    <div id="firste" updated='0'></div>
    <div id="seconde" updated='0'></div>
</div>

</td><td>

<div id="toptimetabs">
	<span class='header'>Top Times</span>
    <ul>
        <li><a href="#toprawcell"><span>Raw</span></a></li>
        <li><a href="#topnetcell"><span>Net</span></a></li>
%for ii in range(1, c.event.getSegmentCount()+1):
        <li><a href="#topseg${ii}cell"><span>Seg ${ii}</span></a></li>
%endfor
    </ul>
    <div id="toprawcell"></div>
    <div id="topnetcell"></div>
%for ii in range(1, c.event.getSegmentCount()+1):
    <div id="topseg${ii}cell"></div>
%endfor
</div>

</td></tr></table>

