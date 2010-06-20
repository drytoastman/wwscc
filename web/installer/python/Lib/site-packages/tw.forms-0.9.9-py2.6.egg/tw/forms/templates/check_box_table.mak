<%namespace name="tw" module="tw.core.mako_util"/>\
<table ${tw.attrs(
    [('class', css_class),
     ('id', context.get('id'))],
    attrs=list_attrs
)}>
	<tbody>
	% for i,row in enumerate(options_rows):
	<tr>
		% for value, desc, attrs in row:
		<td>
			<% id_c = str(id_counter.next()) %>
			<input ${tw.attrs(
				[('type', field_type),
				 ('name', name),
				 ('id', (context.get('id') or '') + '_' + id_c),
				 ('value', value)],
				attrs=attrs
			)} />\
            <label for="${(context.get('id') or '')}_${id_c}">${tw.content(desc)}</label>
		</td>
		% endfor
        % for j in xrange(num_cols - len(row)):
            <td></td>
        % endfor
	</tr>
    % endfor
	</tbody>
</table>\
