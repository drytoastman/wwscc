<%inherit file="/base.mako" />

<style>
#series { margin-left: 20px; }
#sponsor { margin-left: 20px; }
#login { margin-left: 20px; margin-top: 20px; }
#submit { margin-left: 230px; margin-top: 10px; font-size: 0.8em; }
</style>


<div id='series'>
<h2>${c.settings.seriesname}</h2>
</div>


<div id='sponsor'>
%if c.sponsorlink is not None and c.sponsorlink.strip() != "":
  <a href='${c.sponsorlink}' target='_blank'>
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
  </a>
%else:
  <img src='${h.url_for(controller='db', name='sponsorimage')}' alt='Sponsor Image'/>
%endif
</div>


<form id="loginForm" action="${h.url_for(action='checklogin')}" method="post">
<div id='login'>
<input type="hidden" name="forward" value=""/>
<table class='form'>
<tr><th>First Name</th><td><input type="text" name="firstname" value="" class="required"/></td></tr>
<tr><th>Last Name</th><td><input type="text" name="lastname" value="" class="required"/></td></tr>
<tr><th>Email or Unique Id</th><td><input type="text" name="email" value="" class="required"/></td></tr>
</table>
</div>

<div id='submit'>
<input type="submit" value="Submit" id='loginsubmit'/>
</div>
</form>


<script>
  $(document).ready(function(){ $("#loginForm").validate(); });
  $(document).ready(function(){ $("#loginsubmit").button(); });
</script>

