// static/js/expert/expert_terms_step1.js
document.addEventListener('DOMContentLoaded', () => {
  const btnNext   = document.getElementById('btnSubmit'); // 기존 약관 페이지의 "회원가입(다음)" 버튼 재사용
  const btnCancel = document.getElementById('btnCancel');
  const radios    = document.querySelectorAll('input[name="termsAgree"]');

  // 약관 동의 전에는 버튼 비활성화
  const updateNextDisabled = () => {
    const agree = document.querySelector('input[name="termsAgree"]:checked')?.value === 'Y';
    btnNext.disabled = !agree;
  };
  radios.forEach(r => r.addEventListener('change', updateNextDisabled));
  updateNextDisabled();

  // 취소 → 홈
  btnCancel.addEventListener('click', () => {
    if (confirm('회원가입을 취소하시겠습니까?')) {
      sessionStorage.removeItem('expertConsent');
      window.location.href = '/';
    }
  });

  // 다음(= Step2 이동)
  btnNext.addEventListener('click', (e) => {
    e.preventDefault();
    const agree = document.querySelector('input[name="termsAgree"]:checked')?.value;
    if (agree !== 'Y') {
      alert('약관에 동의해야 계속 진행할 수 있습니다.');
      return;
    }
    // 동의 결과 저장
    sessionStorage.setItem('expertConsent', JSON.stringify({ termsAgree: 'Y' }));
    // Step2로 이동 (전문가 정보 입력 페이지 라우트로 맞춰 주세요)
    window.location.href = '/expertSignupPage';
  });
});
