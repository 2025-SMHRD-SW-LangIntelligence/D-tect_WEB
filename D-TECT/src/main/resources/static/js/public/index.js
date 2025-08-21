// /js/public/index.js

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('loginForm');
  const usernameEl = document.getElementById('username');
  const passwordEl = document.getElementById('password');
  const submitBtn  = form?.querySelector('button[type="submit"]');

  // ✅ 기본 로그인 폼 검증 (정상 제출 허용)
  if (form && usernameEl && passwordEl) {
    form.addEventListener('submit', (e) => {
      const username = usernameEl.value.trim();
      const password = passwordEl.value.trim();

      if (!username || !password) {
        e.preventDefault(); // 검증 실패 시에만 막음
        alert('아이디와 비밀번호를 입력해 주세요.');
        (!username ? usernameEl : passwordEl).focus();
        return;
      }

      // 중복 제출 방지(선택)
      submitBtn?.setAttribute('disabled', 'disabled');
      submitBtn?.classList.add('is-loading'); // 필요하면 CSS로 로딩 스타일
      // ⚠️ 여기서는 preventDefault()를 하지 않음 → 스프링 시큐리티가 /login POST를 처리
    });
  }

  // ✅ Google / Kakao OAuth (Spring Security OAuth2 사용 시)
  const googleBtn = document.getElementById('googleLogin');
  const kakaoBtn  = document.getElementById('kakaoLogin');

  googleBtn?.addEventListener('click', () => {
    // 등록한 client (registrationId)가 'google'일 때
    window.location.href = '/oauth2/authorization/google';
  });

  kakaoBtn?.addEventListener('click', () => {
    // 등록한 client (registrationId)가 'kakao'일 때
    window.location.href = '/oauth2/authorization/kakao';
  });
});
