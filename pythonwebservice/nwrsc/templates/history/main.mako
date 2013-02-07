<%inherit file="/base.mako" />

<form id='historyform' action='${h.url_for(action="report")}' method='post'>

<fieldset id='istinput'>
<legend>IST Qualifications</legend>

<label for='isttotal'>Total Event Max</label>
<input type='number' name='isttotal' id='isttotal' value='10'/>
<br class='after'/>

<label for='istavg'>Average Events/Year Max</label>
<input type='number' name='istavg' id='istavg' value='3'/>
<br class='after'/>

</fieldset>



<fieldset id='pcinput'>
<legend>PunchCard Qualifications</legend>

<label for='pcseries'>Include Series</label>
<input type='text' name='pcseries' id='pcseries' value='nwr,pro'/>
<br class='after'/>

<label for='pcyearmax'>Max Events Per Year</label>
<input type='number' name='pcyearmax' id='pcyearmax' value='4'/>
<br class='after'/>

<label for='pcsinceyear'>Since</label>
<input type='number' name='pcsinceyear' id='pcsinceyear' value='${c.recentyear}'/>
<br class='after'/>

<label for='pcchamp'>No Championships Ever</label>
<input type='checkbox' name='pcchamp' id='pcchamp' checked='checked'/>
<br class='after'/>

</fieldset>



<fieldset id='buttons'>

<legend>Output</legend>
<label for='exclusionsonly'>Only List Exclusions</label>
<input type='checkbox' name='exclusionsonly' id='exclusionsonly'/>
<br class='after'/>

%for name, title in (('colyears', 'Years Column'), ('colseries', 'Series Column'), ('colisttotal', 'IST Total Column'), ('colistavg', 'IST Average Column'), ('colpcchamp', 'Punch Champ Column'), ('colpcevents', 'Punch Events Column'), ('colistqualify', 'IST Qualify Column'), ('colpcqualify', 'Punch Qualify Column')):
<label for='${name}'>${title}</label>
<input type='checkbox' name='${name}' id='${name}' checked='checked'/>
<br class='after'/>
%endfor

<label>Output Type</label>
<input type='submit' name='selection' value='Table'/>
<input type='submit' name='selection' value='CSV'/>
<br class='after'/>

</fieldset>

</form>
