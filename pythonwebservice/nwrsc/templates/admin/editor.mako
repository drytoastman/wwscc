<!DOCTYPE html>
<html lang="en">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>Code Editor</title>
	<script language="Javascript" type="text/javascript" src="/editarea/edit_area_full.js"></script>
	<script language="Javascript" type="text/javascript">
		editAreaLoader.init({
			id: "codeeditor", start_highlight: true, allow_resize: "both",
			allow_toggle: false, language: "en", syntax: "python"	
		});
	</script>
</head>
<body>
<form action='${h.url_for(action='savecode')}' method='post'>
<input type='hidden' name='name' value='${c.name}'/>
<textarea id="codeeditor" style="height: 600px; width: 100%;" name="data">${c.data}</textarea>
<input type='submit' value='Save'/>
</form>
</body>
</html>
