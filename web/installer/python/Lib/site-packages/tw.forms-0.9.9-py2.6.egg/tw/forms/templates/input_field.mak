<%namespace name="tw" module="tw.core.mako_util"/>\
<input ${tw.attrs(
        [('type', type),
         ('id', context.get('id')),
         ('class', css_class),
         ('name', name),
         ('value', value)],
        attrs=attrs
    )} />