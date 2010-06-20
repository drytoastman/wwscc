<%namespace name="tw" module="tw.core.mako_util"/>\
<%
    error = context.get('error')
%>\
<fieldset ${tw.attrs(
    [('id', context.get('id')),
     ('class', css_class)],
    attrs=attrs
)}>
    % if legend:
        <legend>${tw.content(legend)}</legend>
    % endif
    % if error and show_error:
    <div class="fielderror">${tw.content(error)}</div>
    % endif
    % if hidden_fields:
    <div>
        % for field in hidden_fields:
            <%
                field_error = error_for(field)
            %>
            ${field.display(value_for(field), **args_for(field))}
            % if show_children_errors and field_error and not field.show_error:
            <span class="fielderror">${tw.content(field_error)}</span>
            % endif
        % endfor
    </div>
    % endif
    <table border="0" cellspacing="0" cellpadding="2" ${tw.attrs(attrs=table_attrs)}>
        <tbody>
            % for i, field in enumerate(fields):
            <tr class="${i%2 and 'odd' or 'even'}" id="${field.id}.container" \
                    title="${hover_help and help_text or ''}" \
                    ${tw.attrs(args_for(field).get('container_attrs') or field.container_attrs)}>\
<%
    required = ['',' required'][int(field.is_required)]
    field_error = error_for(field)
    label_text = field.label_text
    help_text = field.help_text
%>
% if show_labels and not field.suppress_label:
                    <td class="labelcol">
                        % if label_text:
                        <label ${tw.attrs(
                            [('id', '%s.label' % field.id),
                             ('for', field.id),
                             ('class', 'fieldlabel%s' % required)]
                        )}>${tw.content(label_text)}</label>
                        % endif
                    </td>
% endif
                <td class="fieldcol" ${tw.attrs(show_labels and field.suppress_label and dict(colspan=2))}>
                    ${field.display(value_for(field), **args_for(field))}
                    % if help_text and not hover_help:
                    <span class="fieldhelp">${tw.content(help_text)}</span>
                    % endif
                    % if show_children_errors and field_error and not field.show_error:
                    <span class="fielderror">${tw.content(field_error)}</span>
                    % endif
                </td>
            </tr>
            % endfor
        </tbody>
    </table>
    % if error and not error.error_dict:
    <span class="fielderror">${tw.content(error)}</span>
    % endif
</fieldset>\
