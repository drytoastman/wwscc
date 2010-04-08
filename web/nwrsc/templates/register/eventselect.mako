<%inherit file="base.mako" />

%for e in sorted(c.events, key=lambda obj: obj.date):
<div>
<a href='${h.url_for(event=e.id)}'>${e.name}</a>
<br>
%if e.totlimit:
${e.count}/${e.totlimit} entries
%else:
${e.count} entries
%endif
</div>
%endfor

