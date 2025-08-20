// 기본 로그인 폼 검증(데모)
const form = document.getElementById('loginForm');
form.addEventListener('submit', (e) => {
    e.preventDefault();
    const id = document.getElementById('userid').value.trim();
    const pw = document.getElementById('pwd').value.trim();

    if (!id || !pw) {
        alert('아이디와 비밀번호를 입력해 주세요.');
        return;
    }
    // TODO: 실제 로그인 호출
    console.log('로그인 요청:', { id, pw });
});

// Google / Kakao OAuth 버튼 핸들러(후에 API 연동)
document.getElementById('googleLogin').addEventListener('click', () => {
    // TODO: 구글 OAuth 호출 위치
    console.log('Google OAuth start');
});

document.getElementById('kakaoLogin').addEventListener('click', () => {
    // TODO: 카카오 OAuth 호출 위치
    console.log('Kakao OAuth start');
});
