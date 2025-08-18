<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="c" uri="http://jakarta.ee/xml/ns/jakartaee/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>로그인</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
</head>
<body>
    <h2>로그인</h2>
    <form action = "${pageContext.request.contextPath}/member/login" method="post">
        <label for="username">아이디:</label>
        <input type="text" id="username" name="username" required /><br/><br/>

        <label for="password">비밀번호:</label>
        <input type="password" id="password" name="password" required /><br/><br/>

        <button type="submit">로그인</button>
    </form>

    <p>계정이 없으신가요? <a href="${pageContext.request.contextPath}/signup">회원가입</a></p>

    <c:if test = "${not empty param.error}">
        <p style = "color:red;">로그인 실패: 아이디 또는 비밀번호를 확인하세요.</p>
    </c:if>
</body>
</html>
