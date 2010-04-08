<%namespace name="tw" module="tw.core.mako_util"/>
<%
	strip = True
	import tw.forms as twf
%>

<form ${tw.attrs(
		[('id', context.get('id')),
		('name', name),
		('action', action),
		('method', method),
		('class', css_class + " autogrow")],
		attrs=attrs)}>
    <div>
		%for field in hidden_fields:
        <div>
            ${field.display(value_for(field), **args_for(field))}
            %if show_children_errors and error and not field.show_error:
            	<span class="fielderror">${error_for(field)}</span>
			%endif
        </div>
		%endfor
    </div>

    <table border="0" cellspacing="0" cellpadding="2" py:attrs="table_attrs">
        <tbody id="${id}_repeater">
        <tr>
            %for ch in children['grow'].widget.children:
				%if not isinstance(ch, twf.HiddenField):
            		<th title="${ch.help_text}">${ch.label_text or ""}</th>
				%endif
			%endfor

        <td><input style="display:none" type="image" id="${id}_undo" src="${undo_url}" alt="Undo" onclick="twd_grow_undo(this); return false;"/></td>
        </tr>

        ${children['grow'].display(value_for(children['grow']) or value, **args_for(children['grow']))}
        </tbody>
    </table>
    <table style="display:none">${display_child('spare')}</table>

    <p>${children['submit'].display()}</p>
</form>

