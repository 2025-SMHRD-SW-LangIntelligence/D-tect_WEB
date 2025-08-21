// static/js/user/user_terms_step1.js
document.addEventListener('DOMContentLoaded', () => {
  const btnNext   = document.getElementById('btnSubmit'); // 기존 약관 페이지의 버튼 재사용
  const btnCancel = document.getElementById('btnCancel');
  const radios    = document.querySelectorAll('input[name="termsAgree"]');

  const updateNextDisabled = () => {
    const agree = document.querySelector('input[name="termsAgree"]:checked')?.value === 'Y';
    btnNext.disabled = !agree;
  };
  radios.forEach(r => r.addEventListener('change', updateNextDisabled));
  updateNextDisabled();

  btnCancel.addEventListener('click', () => {
    if (confirm('회원가입을 취소하시겠습니까?')) {
      sessionStorage.removeItem('userConsent');
      window.location.href = '/';
    }
  });

  btnNext.addEventListener('click', (e) => {
    e.preventDefault();
    const agree = document.querySelector('input[name="termsAgree"]:checked')?.value;
    if (agree !== 'Y') { alert('약관에 동의해야 계속 진행할 수 있습니다.'); return; }
    sessionStorage.setItem('userConsent', JSON.stringify({ termsAgree: 'Y' }));
    window.location.href = '/userSignupPage'; // 일반 사용자 정보 입력 페이지로 맞춰 주세요
  });
});
