<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>
<html xmlns='http://www.w3.org/1999/xhtml'>
<head>
<title>${c.title}</title>
<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />
%for style in c.stylesheets:
<link href="${style}" rel="stylesheet" type="text/css" />
%endfor
%for js in c.javascript:
${h.javascript_link(js)}
%endfor
<%def name="extrahead()"></%def>
${self.extrahead()}
</head>
<body>
${c.header|n}
${next.body()}
</body>
</html>
