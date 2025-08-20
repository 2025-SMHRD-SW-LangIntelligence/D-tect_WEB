// expert_signup.js
import { validatePasswords, setupPhoneValidation, checkUsername, setupEmailVerification, setupAddressSearch } from '/js/public/common.js';

document.addEventListener('DOMContentLoaded', () => {
    // DOM 요소
    const usernameEl = document.getElementById('username');
    const btnCheckUsername = document.getElementById('btnCheckUsername');
    const passwordEl = document.getElementById('password');
    const password2El = document.getElementById('password2');
    const passwordMsgEl = document.getElementById('passwordMsg');
    const emailEl = document.getElementById('email');
    const btnSendCode = document.getElementById('btnSendCode');
    const btnVerifyCode = document.getElementById('btnVerifyCode');
    const codeEl = document.getElementById('emailCode');
    const codeMsgEl = document.getElementById('emailMsg');
    const phoneEl = document.getElementById('phone');
    const btnSearchAddr = document.getElementById('btnSearchAddr');
    const addressEl = document.getElementById('address');

    // 기능 설정
    setupPhoneValidation(phoneEl);
    checkUsername(btnCheckUsername, usernameEl);
    setupEmailVerification(btnSendCode, btnVerifyCode, emailEl, codeEl, codeMsgEl);
    setupAddressSearch(btnSearchAddr, addressEl);

    // 비밀번호 일치 여부 실시간 체크
    password2El.addEventListener('input', () => {
        validatePasswords(passwordEl, password2El, passwordMsgEl);
    });

    // 다음 버튼 클릭 시 sessionStorage 저장 후 약관 페이지 이동
    document.getElementById('btnNext').addEventListener('click', () => {
        if (!validatePasswords(passwordEl, password2El, passwordMsgEl)) return;

        const formData = {
            name: document.getElementById('name').value,
            username: usernameEl.value,
            password: passwordEl.value,
            email: emailEl.value,
            phone: phoneEl.value,
            address: addressEl.value,
            addrDetail: document.getElementById('addrDetail').value,
            officeName: document.getElementById('officeName')?.value || '',
            officeAddress: document.getElementById('officeAddress')?.value || ''
        };
        sessionStorage.setItem('expertSignupData', JSON.stringify(formData));
        window.location.href = '/expert/terms';
    });
});
