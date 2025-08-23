import { setupEmailVerification } from '/js/public/common.js';

// 이동 경로 설정
const AFTER_SUCCESS = '/loginPage'; // 변경 완료 후 이동할 페이지

// 요소
const form = document.getElementById('verifyForm');
const btnEmail = document.getElementById('btnEmail');
const phone = document.getElementById('phone');
const btnVerify = document.getElementById('btnVerifyEmail');

const modal = document.getElementById('pwModal');
const newPwd = document.getElementById('newPwd');
const newPwd2 = document.getElementById('newPwd2');
const meter = document.getElementById('meter');
const matchMsg = document.getElementById('matchMsg');
const confirmB = document.getElementById('confirmChange');
const cancelB = document.getElementById('cancelChange');
const toggle1 = document.getElementById('toggle1');
const toggle2 = document.getElementById('toggle2');
const emailEl = document.getElementById('email');
const emailCodeEl = document.getElementById('emailCode');
const emailMsgEl = document.getElementById('emailMsg');

// 이메일 인증
setupEmailVerification(btnEmail, btnVerify, emailEl, emailCodeEl, emailMsgEl);

// 전화번호 숫자만, 11자리 제한
phone.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 11);
    e.target.setCustomValidity('');
});

// 1) 정보 검증 후 모달 열기
form.addEventListener('submit', (e) => {
    e.preventDefault();
    // 기본 HTML 유효성
    if (!form.reportValidity()) return;

    // 전화번호 최종 체크
    if (!/^[0-9]{10,11}$/.test(phone.value)) {
        phone.setCustomValidity('전화번호는 숫자 10~11자리(하이픈 없이)로 입력해 주세요.');
        phone.reportValidity(); return;
    } else { phone.setCustomValidity(''); }

    openModal();
});

function openModal() {
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
    newPwd.value = '';
    newPwd2.value = '';
    meter.textContent = '';
    matchMsg.textContent = '';
    setTimeout(() => newPwd.focus(), 0);
}
function closeModal() {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
}

// 2) 비밀번호 강도/일치 검증
newPwd.addEventListener('input', updateStrength);
newPwd2.addEventListener('input', checkMatch);

function updateStrength() {
    const p = newPwd.value;
    meter.className = 'meter';
    if (!p) { meter.textContent = ''; return; }

    // 강도 간단 평가: 길이 + 종류
    const len = p.length >= 8;
    const hasNum = /\d/.test(p);
    const hasEng = /[a-zA-Z]/.test(p);
    const hasSpec = /[^a-zA-Z0-9]/.test(p);

    let score = (len ? 1 : 0) + (hasNum ? 1 : 0) + (hasEng ? 1 : 0) + (hasSpec ? 1 : 0);

    if (score >= 4) { meter.textContent = '강함'; meter.classList.add('good'); }
    else if (score >= 3) { meter.textContent = '보통'; meter.classList.add('ok'); }
    else { meter.textContent = '약함'; meter.classList.add('weak'); }

    checkMatch();
}
function checkMatch() {
    matchMsg.className = 'match';
    if (!newPwd2.value) { matchMsg.textContent = ''; return; }

    if (newPwd.value === newPwd2.value && newPwd.value.length >= 8) {
        matchMsg.textContent = '일치합니다.'; matchMsg.classList.add('ok');
        newPwd2.setCustomValidity('');
    } else {
        matchMsg.textContent = '비밀번호가 일치하지 않습니다.'; matchMsg.classList.add('err');
        newPwd2.setCustomValidity('비밀번호가 일치하지 않습니다.');
    }
}

// 3) 표시/숨기기
toggle1.addEventListener('click', () => {
    const t = newPwd.type === 'password' ? 'text' : 'password';
    newPwd.type = t;
});
toggle2.addEventListener('click', () => {
    const t = newPwd2.type === 'password' ? 'text' : 'password';
    newPwd2.type = t;
});

// 4) 모달 확인/취소
confirmB.addEventListener('click', async () => {
    // 최종 검증
    const pwdOK = newPwd.value.length >= 8 &&
        /[a-zA-Z]/.test(newPwd.value) &&
        /\d/.test(newPwd.value);
    if (!pwdOK) {
        alert('비밀번호는 영문과 숫자를 포함하여 8자 이상이어야 합니다.');
        newPwd.focus(); return;
    }
    if (newPwd.value !== newPwd2.value) {
        newPwd2.reportValidity(); return;
    }

    // TODO: 실제 변경 API 호출
    // await api.changePassword({ userid: ..., newPassword: newPwd.value });

    closeModal();
    alert('비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
    location.href = AFTER_SUCCESS;
});

cancelB.addEventListener('click', closeModal);

// ESC로 닫기, 배경 클릭 닫기
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('open')) closeModal();
});
modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
});
