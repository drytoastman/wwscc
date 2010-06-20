<%namespace name="tw" module="tw.core.mako_util"/>\
<%
    attrs = context.get('attrs')
%>\
<select ${tw.attrs(
    [('name', name),
     ('class', css_class),
     ('id', context.get('id'))],
    attrs=attrs
)}>
    % for group, options in grouped_options:
    % if group:
    <optgroup ${tw.attrs([('label', group)])}>
    % endif
        % for value, desc, attrs in options:
        <option ${tw.attrs(
            [('value', value)],
            attrs=attrs
        )}>${tw.content(desc)}</option>
        % endfor
    % if group:
    </optgroup>
    % endif
    % endfor
</select>\
