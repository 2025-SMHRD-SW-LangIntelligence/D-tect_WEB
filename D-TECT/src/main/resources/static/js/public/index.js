// /js/public/index.js

document.addEventListener('DOMContentLoaded', () => {
  // ✅ 백엔드가 넘겨준 auth 플래그 처리
  //    expected: blocked | expert_pending | expert_register
  const params = new URLSearchParams(window.location.search);
  const authFlag = params.get('auth');
	console.log(authFlag)
  if (authFlag) {
    switch (authFlag) {
      case 'blocked':
        alert('차단된 계정입니다. 관리자에게 문의해 주세요.');
        break;
      case 'expert_pending':
        alert('전문가 승인 대기 중입니다. 승인 후 이용할 수 있습니다.');
        break;
      case 'expert_register':
        if (confirm('전문가 등록이 필요합니다. 등록 페이지로 이동할까요?')) {
          window.location.href = '/joinExpertPage';
        }
        break;
      default:
        break;
    }
    // 새로고침 시 alert 반복 방지: 쿼리스트링 제거
    if (window.history && window.history.replaceState) {
      window.history.replaceState({}, '', window.location.pathname);
    }
  }

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
        e.preventDefault();
        alert('아이디와 비밀번호를 입력해 주세요.');
        (!username ? usernameEl : passwordEl).focus();
        return;
      }

      // 중복 제출 방지(선택)
      submitBtn?.setAttribute('disabled', 'disabled');
      submitBtn?.classList.add('is-loading'); // 필요 시 CSS로 로딩 스타일
      // ⚠️ 여기서는 preventDefault() 하지 않음 → 스프링 시큐리티가 /login POST 처리
    });
  }

  // ✅ Google / Kakao OAuth (Spring Security OAuth2 사용 시)
  const googleBtn = document.getElementById('googleLogin');
  const kakaoBtn  = document.getElementById('kakaoLogin');

  if (googleBtn && googleBtn.tagName === 'BUTTON') {
      googleBtn.addEventListener('click', () => {
        window.location.href = '/oauth2/authorization/google';
      });
    }
    if (kakaoBtn && kakaoBtn.tagName === 'BUTTON') {
      kakaoBtn.addEventListener('click', () => {
        window.location.href = '/oauth2/authorization/kakao';
      });
    }
});
