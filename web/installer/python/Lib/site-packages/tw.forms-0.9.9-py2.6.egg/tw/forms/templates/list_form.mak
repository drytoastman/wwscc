<%namespace name="tw" module="tw.core.mako_util"/>\
<%
    error = context.get('error')
%>\
<form ${tw.attrs(
    [('id', context.get('id')),
     ('name', name),
     ('action', action),
     ('method', method),
     ('class', css_class)],
    attrs=attrs
)}>
    % if error and show_error:
    <div class="fielderror">${tw.content(error)}</div>
    % endif
    % if hidden_fields:
    <div>
        % for field in hidden_fields:
            <%
                error = error_for(field)
            %>
            ${field.display(value_for(field), displays_on='mako', **args_for(field))}
            % if show_children_errors and error and not field.show_error:
            <span class="fielderror">${tw.content(error)}</span>
            % endif
        % endfor
    </div>
    % endif
    <ul class="field_list" ${tw.attrs(attrs=list_attrs)}>
        % for i, field in enumerate(fields):
        <li class="${i%2 and 'odd' or 'even'}" id="${field.id}_container" \
                title="${hover_help and help_text or ''}" \
                ${tw.attrs(args_for(field).get('container_attrs') or field.container_attrs)}>\
            <%
                required = ['',' required'][int(field.is_required)]
                error = error_for(field)
                label_text = field.label_text
                help_text = field.help_text
            %>
            % if show_labels and label_text and not field.suppress_label:
            <label ${tw.attrs(
                [('id', '%s_label' % field.id),
                 ('for', field.id),
                 ('class', 'fieldlabel%s' % required)]
            )}>${tw.content(label_text)}</label>
            % endif
            ${field.display(value_for(field), displays_on='mako', **args_for(field))}
            % if help_text and not hover_help:
            <span class="fieldhelp">${tw.content(help_text)}</span>
            % endif
            % if show_children_errors and error and not field.show_error:
            <span class="fielderror">${tw.content(error)}</span>
            % endif
        </li>
        % endfor
    </ul>
    % if error and not error.error_dict:
    <span class="fielderror">${tw.content(error)}</span>
    % endif
</form>\
