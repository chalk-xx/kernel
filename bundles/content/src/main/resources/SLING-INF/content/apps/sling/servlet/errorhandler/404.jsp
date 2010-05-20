<%
  response.setStatus(404);
%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 TRANSITIONAL//EN">
<html>

	<head>
		<title></title>
	</head>
	<body>

<h1>Page Not Found</h1>
<p>
If you are not already logged in you could try logging in with your favorite login mechanism
</p>
<ul>
<li><a href="/trusted">Login with Single Sign On</a></li>
<li><a href="/system/sling/logout">Login with User Name and Password</a></li>
</ul>
<p>
You might also want to logout, which you can do here 
</p>
<p>
<a href="/system/sling/logout">Logout</a>
</p>
	</body>
</html>
