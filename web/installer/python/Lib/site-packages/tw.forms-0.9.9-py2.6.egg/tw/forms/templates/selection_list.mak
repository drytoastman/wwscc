<%namespace name="tw" module="tw.core.mako_util"/>\
<ul ${tw.attrs(
    [('class', css_class),
     ('id', context.get('id'))],
    attrs=list_attrs
)}>
    % for value, desc, attrs in options:
    <li>
		<% id_c = str(id_counter.next()) %>
        <input ${tw.attrs(
            [('type', field_type),
             ('name', name),
             ('id', (context.get('id') or '') + '_' + id_c),
             ('value', value)],
            attrs=attrs
        )} />\
        <label for="${(context.get('id') or '')}_${id_c}">${tw.content(desc)}</label>
    </li>
    % endfor
</ul>\
