<%inherit file="base.mako" />
<h2 style='margin-left:20px'>${c.seriesname}</h2>

<form id="loginForm" action="${h.url_for(action='checklogin')}" method="post">
<input type="hidden" name="forward" value=""/>

<table class='form'>
<tr><th>First Name</th><td><input type="text" name="firstname" value="" class="required"/></td></tr>
<tr><th>Last Name</th><td><input type="text" name="lastname" value="" class="required"/></td></tr>
<tr><th>Email or Unique Id</th><td><input type="text" name="email" value="" class="required"/></td></tr>
</table>
<input type="submit" value="Submit"/>

</form>


<script>
  $(document).ready(function(){ $("#loginForm").validate(); });
</script>

<style>
