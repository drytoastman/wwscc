<form \
type="${context.get('type')}" \
name="${name}" \
class="${attrs.get('class') or ''}" \
id="${context.get('id')}" \
value="${value}" \
% for k, v in attrs.iteritems():
  % if v is not None:
${k}="${unicode(attrs.get(k)) or ''}" \
  % endif
% endfor
/>
    <div>
    % for field in hidden_fields:
        ${field.display(value_for(field), displays_on='mako', **args_for(field))}
    % endfor
    % for field in fields:
        ${field.display(value_for(field), displays_on='mako', **args_for(field))} <br />
    % endfor
    </div>
</form>\
