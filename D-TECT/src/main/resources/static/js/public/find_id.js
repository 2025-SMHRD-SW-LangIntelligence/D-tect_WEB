import { setupEmailVerification } from '/js/public/common.js';

const emailEl = document.getElementById('email')
const form = document.getElementById('findForm');
const btnVerify = document.getElementById('btnVerifyEmail');
const btnEmail = document.getElementById('btnEmail');
const phone = document.getElementById('phone');
const result = document.getElementById('result');
const resultText = document.getElementById('resultText');
const emailCodeEl = document.getElementById('emailCode');
const emailMsgEl = document.getElementById('emailMsg');

// 전화번호: 숫자만, 11자리 제한
phone.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 11);
    // input 단계에서는 커스텀 메시지 비움
    e.target.setCustomValidity('');
});

setupEmailVerification(btnEmail, btnVerify, emailEl, emailCodeEl, emailMsgEl);

// 찾기(데모)
form.addEventListener('submit', (e) => {
    e.preventDefault();

    // HTML 기본 검증
    if (!form.reportValidity()) return;

    // 최종 전화번호 패턴 체크
    const ok = /^[0-9]{10,11}$/.test(phone.value);
    if (!ok) {
        phone.setCustomValidity('전화번호는 숫자 10~11자리(하이픈 없이)로 입력해 주세요.');
        phone.reportValidity();
        return;
    }
    phone.setCustomValidity('');

    // TODO: 실제 서버 조회
    // 데모: 이름 + 전화 뒤 4자리 기반 마스킹된 아이디 생성
    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const tail = phone.value.slice(-4);
    const masked = `${name || 'user'}***_${tail}`;

    resultText.textContent = `회원님의 아이디는 "${masked}" 입니다.`;
    result.hidden = false;
});
