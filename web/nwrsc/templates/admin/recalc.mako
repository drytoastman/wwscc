<%inherit file="base.mako" />

<h3>Recalculating Results</h3>

<div id='output'>
Starting...
</div>

<script>

$('#output').html('Waiting for results ... (up to a minute)')
$('#output').load('${h.url_for(action="dorecalc")}');

</script>

