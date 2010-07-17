<%inherit file="base.mako" />
<%def name="extrahead()">
</%def>

<pre>
%for group in c.groups:
--- ${group.groupnum} ---
%for cls in group.classes:
${cls.code}
%for e in sorted(cls.first, key=lambda e: e.car.number):
${e.driver.firstname} ${e.driver.lastname} - ${e.car.indexcode}, ${e.car.number}
%endfor
   --
%for e in sorted(cls.second, key=lambda e: e.car.number):
${e.driver.firstname} ${e.driver.lastname} - ${e.car.indexcode}, ${e.car.number}
%endfor
%endfor
%endfor

</pre>

