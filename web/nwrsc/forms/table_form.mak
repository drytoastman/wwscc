<%namespace name="tw" module="tw.core.mako_util"/>
<form ${tw.attrs(
    [('id', context.get('id')),
     ('name', name),
     ('action', action),
     ('method', method),
     ('class', css_class)],
    attrs=attrs
)}>
    % if hidden_fields:
    <div>
        % for field in hidden_fields:
            ${field.display(value_for(field), **args_for(field))}
        % endfor
    </div>
    % endif

    <table border="0" cellspacing="0" cellpadding="2" ${tw.attrs(attrs=table_attrs)}>
        % for i, field in enumerate(fields):
        <tr class="${i%2 and 'odd' or 'even'}" id="${field.id}.container" title="${field.help_text}" \
                ${tw.attrs(args_for(field).get('container_attrs') or field.container_attrs)}>\
            <%
                required = ['',' required'][int(field.is_required)]
                error = error_for(field)
            %>
            % if show_labels and not field.suppress_label:
            <td class="labelcol">
                <label ${tw.attrs(
                    [('id', '%s.label' % field.id),
                     ('for', field.id),
                     ('class', 'fieldlabel%s' % required)]
                )}>${tw.content(field.label_text)}</label>
            </td>
            % endif
            <td class="fieldcol" ${tw.attrs(show_labels and field.suppress_label and dict(colspan=2))}>
                ${field.display(value_for(field), **args_for(field))}
                % if show_children_errors and error and not field.show_error:
                <span class="fielderror">${tw.content(error)}</span>
                % endif
            </td>
        </tr>
        % endfor
    </table>
    % if error and not error.error_dict:
    <span class="fielderror">${tw.content(error)}</span>
    % endif
</form>
