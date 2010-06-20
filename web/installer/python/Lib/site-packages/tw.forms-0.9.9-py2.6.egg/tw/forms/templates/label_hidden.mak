<%namespace name="tw" module="tw.core.mako_util"/>\
<div ${tw.attrs(
    [('class', css_class)],
    attrs=attrs
    )}>\
${value}
<input type="hidden" ${tw.attrs(
    [('name', name),
     ('id', context.get('id')),
     ('value', value)],
)} />\
</div>\
