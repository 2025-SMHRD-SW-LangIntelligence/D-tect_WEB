<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>회원가입</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" />
</head>
<body>
    <h2>회원가입</h2>
    <form action="${pageContext.request.contextPath}/member/signup" method="post">
        <label for="username">아이디:</label>
        <input type="text" id="username" name="username" required /><br/><br/>

        <label for="password">비밀번호:</label>
        <input type="password" id="password" name="password" required /><br/><br/>

        <label for="name">이름:</label>
        <input type="text" id="name" name="name" required /><br/><br/>

        <label for="addr">주소:</label>
        <input type="text" id="addr" name="addr" required /><br/><br/>

        <label for="email">이메일:</label>
        <input type="email" id="email" name="email" required /><br/><br/>

        <label>
            <input type="checkbox" id="terms_agree" name="terms_agree" value="Y" required />
            약관에 동의합니다.
        </label><br/><br/>

        <label for="mem_role">회원 유형:</label>
        <select id="mem_role" name="mem_role" required>
            <option value="USER">사용자</option>
            <option value="EXPERT">전문가</option>
            <option value="ADMIN">관리자</option>
        </select><br/><br/>

        <label for="office_name">사무실 이름:</label>
        <input type="text" id="office_name" name="office_name" required /><br/><br/>

        <button type="submit">회원가입</button>
    </form>

    <p><a href="${pageContext.request.contextPath}/LoginPage">로그인 페이지로 돌아가기</a></p>

    <c:if test="${not empty errorMessage}">
        <p style="color:red;">${errorMessage}</p>
    </c:if>
</body>
</html>
