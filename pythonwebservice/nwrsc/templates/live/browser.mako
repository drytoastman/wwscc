<!DOCTYPE html> 
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1"> 
	<title>Live Results</title> 
	<link rel="stylesheet" href="/css/live.css" />
	<script src="/js/live.js"></script>
<script type='text/javascript'>
<%
try:
	context.write('$.nwr.urlbase = "%s";' % h.url_for(action=''));
except:
	pass
%>
</script>
</head> 

<body> 

<div data-role="page" id="browserpage" data-theme="b">

	<div data-role="panel" id="classpanel" data-display="overlay">

		<a href="#my-header" data-rel="close" data-role='button'>Close</a>

		<div data-role="fieldcontain" id='settingscheck'>
			Other:
		 	<fieldset data-role="controlgroup">
				<label for="checkbox-pax">PAX Top Times</label> <input type="checkbox" name="pax" id="checkbox-pax"/>
				<label for="checkbox-raw">Raw Top Times</label> <input type="checkbox" name="raw" id="checkbox-raw"/>
				<label for="champflip">Include Champ</label>    <input type="checkbox" name="champon" id="champflip" />
			</fieldset>
		</div>

		<div data-role="fieldcontain" id='classescheck'>
			Classes:
		 	<fieldset data-role="controlgroup">
				%for code in c.classes:
				<label for="checkbox-${code}">${code}</label>
				<input type="checkbox" name="${code}" id="checkbox-${code}"/>
				%endfor
		    </fieldset>
		</div>

	</div>

	<div data-role="header" data-id="header" data-position="fixed" id="header">
		<h1>${c.event.name}</h1> 
		<a href="#classpanel" data-icon="bars" data-iconpos="notext"></a>

		<div data-role="navbar" id='navcontainer'>
			<ul>
				<li><a href='#'>D</a></li>
				<li><a href='#'>E</a></li>
				<li><a href='#'>F</a></li>
			</ul>
		</div>
	</div>

	<div role="main" class="ui-content" id="main">
		<div id='instructions'> If you are reading this, something hasn't loaded yet </div>
	</div>

</div>

</body>
</html>
