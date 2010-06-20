<%namespace name="tw" module="tw.core.mako_util"/>\
<table ${tw.attrs(
    [('id', context.get('id'))],
)} class="${css_class}" cellpadding="0" cellspacing="1" border="0">
    % if columns:
    <thead>
        <tr>
            % for i, col in enumerate(columns):
            <th class="col_${i}">${col.title}</th>
            % endfor
        </tr>
    </thead>
    % endif
    <tbody>
        % for i, row in enumerate(value):
        <tr class="${i%2 and 'odd' or 'even'}">
            % for col in columns:
            <td ${tw.attrs(
    [('align', col.get_option('align', None)),
     ('class', col.get_option('css_class', None))],
)}>${col.get_field(row)}</td>
            % endfor
        </tr>
        % endfor
    </tbody>
</table>\
