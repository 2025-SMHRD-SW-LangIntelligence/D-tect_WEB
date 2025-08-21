// static/js/user/user_signup_step2.js
import {
  validatePasswords,
  setupPhoneValidation,
  checkUsername,
  setupEmailVerification,
  setupAddressSearch
} from '/js/public/common.js';

document.addEventListener('DOMContentLoaded', () => {
  // 1) 동의 체크
  let consent = null;
  try {
    consent = JSON.parse(sessionStorage.getItem('userConsent') || '{}');
  } catch (_) {}
  if (consent?.termsAgree !== 'Y') {
    alert('약관 동의 후 진행해 주세요.');
    window.location.href = '/userTermPage'; // 일반 약관 라우트로 맞춰 주세요
    return;
  }

  // 2) 엘리먼트
  const form        = document.getElementById('userForm');
  const usernameEl  = document.getElementById('username');
  const passwordEl  = document.getElementById('password');
  const password2El = document.getElementById('password2');
  const pwdMsgEl    = document.getElementById('pwdMsg');
  const nameEl      = document.getElementById('name');
  const emailEl     = document.getElementById('email');
  const emailCodeEl = document.getElementById('emailCode');
  const emailMsgEl  = document.getElementById('emailMsg');
  const phoneEl     = document.getElementById('phone');
  const addressEl   = document.getElementById('address');
  const addrDetailEl= document.getElementById('addrDetail');

  const btnCheckId  = document.getElementById('btnCheckId');
  const btnEmail    = document.getElementById('btnEmail');
  const btnVerify   = document.getElementById('btnVerifyEmail');
  const btnAddr     = document.getElementById('btnAddr');
  const btnSubmit   = document.getElementById('btnSubmit');
  const btnCancel   = document.getElementById('btnCancel');

  // 3) 공통 바인딩
  setupPhoneValidation(phoneEl);
  passwordEl.addEventListener('input', () => validatePasswords(passwordEl, password2El, pwdMsgEl, false));
  password2El.addEventListener('input', () => validatePasswords(passwordEl, password2El, pwdMsgEl, true));
  checkUsername(btnCheckId, usernameEl);
  setupEmailVerification(btnEmail, btnVerify, emailEl, emailCodeEl, emailMsgEl);
  setupAddressSearch(btnAddr, addressEl); // 일반: address 사용

  // 4) 취소
  btnCancel?.addEventListener('click', () => {
    if (confirm('회원가입을 취소하시겠습니까?')) {
      sessionStorage.removeItem('userConsent');
      window.location.href = '/';
    }
  });

  // 5) 제출
  btnSubmit?.addEventListener('click', async (e) => {
    e.preventDefault();

    if (!validatePasswords(passwordEl, password2El, pwdMsgEl, true)) {
      password2El.reportValidity();
      return;
    }
    if (!form.reportValidity()) return;

    const payload = {
      isExpert: false,
      termsAgree: 'Y',
      username: usernameEl.value.trim(),
      password: passwordEl.value,
      name:     nameEl.value.trim(),
      email:    emailEl.value.trim(),
      phone:    phoneEl.value.trim(),
      address:  addressEl.value.trim(),
      addrDetail: addrDetailEl.value.trim()
    };

    const fd = new FormData();
    fd.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));

    try {
      const res = await fetch('/api/members/signup', { method: 'POST', body: fd });
      const json = await res.json().catch(() => ({}));
      if (!res.ok || json.success === false) {
        throw new Error(json.message || '회원가입에 실패했습니다.');
      }
      alert('✅ 회원가입 완료!');
      sessionStorage.removeItem('userConsent');
      window.location.href = '/loginPage';
    } catch (err) {
      console.error(err);
      alert(`회원가입 실패: ${err.message}`);
    }
  });
});
