<%namespace name="tw" module="tw.core.mako_util"/>\
<div>
    <input type="text" ${tw.attrs(
        [('id', context.get('id')),
         ('class', css_class),
         ('name', name),
         ('value', strdate)],
        attrs=attrs
    )} />
    <input type="button" class="date_field_button" ${tw.attrs(
        [('id', '%s_trigger' % context.get('id')),
         ('value', button_text)],
        attrs=attrs
    )} />
</div>\
