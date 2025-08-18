<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix = "sec" uri = "http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>메인 페이지</title>
</head>
<body>
	
	<h2><sec:authentication property="principal.username"/>님 환영합니다</h2>
	
	<a href = "/user/logout">로그아웃</a>
	
	
</body>
</html>