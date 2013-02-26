<%inherit file="/base.mako" />

The restricted index is interpreted based on the following values:
<ul>
	<li>() represents a list that applies to the available indexes</li>
	<li>[] represents a list that applies to the additional multiplier flag</li>
	<li>+ before a list indicates that these indexes are allowed</li>
	<li>- before a list indicates that these indexes are NOT allowed</li>
	<li>* can be used as a wildcard character to match index values</li>
	<li>the first () or [] starts the inital list, + starts blank and adds indexes, - starts will the entire list and removes indexes</li>
</ul>

Examples:
<ol>
<li>+(SA,SB,SC) means only SA,SB or SC can be selected as an index.</li>
<li>-(SA,SB,SC) means anything _except_ SA,SB or SC can be selected as an index.</li>
<li>+[SA,SB,SC] means only SA,SB,SC can use carflag if the class provides it.</li>
<li>-[SA,SB,SC] means anything _except_ SA,SB or SC can use carflag if the class provides it.</li>
<li>+(S*) -[ST*] means only indexes that start with S can be selected as an index but cars that start with ST won't be allowed to select carflag.</li>
<li>+(S*,A*)-(ST*) means only indexes that start with S and A but NOT ST can be selected as an index</li>
</ol>


