import {
    validatePasswords, setupPhoneValidation, checkUsername,
    setupEmailVerification, setupAddressSearch, loadDraft
} from '/js/public/common.js';

const API_BASE = '/api/members';
const LOGIN_PATH = '/loginPage';

function csrf() {
    const h = document.querySelector('meta[name="_csrf_header"]')?.content;
    const t = document.querySelector('meta[name="_csrf"]')?.content;
    return h && t ? { [h]: t } : {};
}

const form = document.getElementById('signupForm');
const nameEl = document.getElementById('name');
const usernameEl = document.getElementById('username');
const passwordEl = document.getElementById('password');
const password2El = document.getElementById('password2');
const emailEl = document.getElementById('email');
const emailCodeEl = document.getElementById('emailCode');
const phoneEl = document.getElementById('phone');
const officeAddressEl = document.getElementById('officeAddress');
/* 수정되었ㄷ고: HTML의 상세주소 id가 'addr2' 이므로 해당 엘리먼트를 사용 */
const addrDetailEl = document.getElementById('addr2'); // 있을 경우
const checkIdBtn = document.getElementById('checkIdBtn');
const verifyEmailBtn = document.getElementById('verifyEmailBtn');
const btnVerifyEmail = document.getElementById('btnVerifyEmail');
const addrBtn = document.getElementById('addrBtn');
const pwdMsg = document.getElementById('pwdMsg');
const emailMsg = document.getElementById('emailMsg');

const fileBtn = document.getElementById('fileBtn');
const certFile = document.getElementById('certFile');
const certFileName = document.getElementById('certFileName');

document.getElementById('logoLink')?.addEventListener('click', (e)=>{e.preventDefault();location.href=LOGIN_PATH;});

// 공통 바인딩
setupPhoneValidation(phoneEl);
passwordEl.addEventListener('input', () => validatePasswords(passwordEl, password2El, pwdMsg, true));
password2El.addEventListener('input', () => validatePasswords(passwordEl, password2El, pwdMsg, true));
/* 수정되었ㄷ고: 주소 검색 모듈에도 상세주소 id 'addr2'를 전달 */
setupAddressSearch(addrBtn, officeAddressEl, 'addr2');

// step1 초안(약관/비번 등) 로드
const draft = loadDraft('signup:step1');
if (draft?.password && !passwordEl.value) passwordEl.value = draft.password;
if (draft?.termsAgree === 'Y') window.__termsAgree = 'Y';

// 상태
let isUsernameChecked=false, isEmailVerified=false;

// 아이디/이메일 공통 모듈 + 이벤트
checkUsername(checkIdBtn, usernameEl);
usernameEl.addEventListener('username:checked', (e)=>{ isUsernameChecked = !!e.detail?.available; });

setupEmailVerification(verifyEmailBtn, btnVerifyEmail, emailEl, emailCodeEl, emailMsg);
emailEl.addEventListener('email:verified', (e)=>{ isEmailVerified = !!e.detail?.success; });

// 파일
fileBtn?.addEventListener('click', ()=>certFile.click());
certFile?.addEventListener('change', ()=>{ certFileName.value = certFile.files?.[0]?.name || ''; });

// ===== 전문분야 칩 생성/선택 =====
/* 수정되었ㄷ고: Enum 코드 ↔ 한글 라벨 매핑을 요구하신 그대로 적용 */
const CODE_TO_LABEL = {
    VIOLENCE:  '폭력',
    DEFAMATION:'명예훼손',
    STALKING:  '스토킹',
    SEXUAL:    '성범죄',
    LEAK:      '정보유출',
    BULLYING:  '따돌림/집단괴롭힘',
    CHANTAGE:  '협박/갈취',
    EXTORTION: '공갈/강요'
};

const chipsWrap = document.getElementById("specialtyChips");
/* 수정되었ㄷ고: 선택 상태는 Enum 코드(Set<string>) 기준으로 관리 */
let selectedCodes = new Set();

function renderChips() {
    chipsWrap.innerHTML = Object.entries(CODE_TO_LABEL).map(([code, label]) => {
        const sel = selectedCodes.has(code);
        return `<button type="button" class="chip${sel ? ' is-selected' : ''}" data-code="${code}" aria-pressed="${sel?'true':'false'}">${label}</button>`;
    }).join("");
}
renderChips();

chipsWrap.addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    const code = chip.dataset.code;
    if (!code) return;

    if (selectedCodes.has(code)) selectedCodes.delete(code);
    else selectedCodes.add(code);

    renderChips();
});

// 제출
form.addEventListener('submit', async (e)=>{
    e.preventDefault();

    const dto = {
        username: usernameEl.value.trim(),
        password: (passwordEl.value||'').trim(),
        name: nameEl.value.trim(),
        phone: phoneEl.value.trim(),
        email: emailEl.value.trim(),
        address: officeAddressEl.value.trim(),                  // 공통 주소
        /* 수정되었ㄷ고: 상세주소 id 일치 */
        addrDetail: (addrDetailEl?.value||'').trim(),
        termsAgree: window.__termsAgree === 'Y' ? 'Y' : 'N',
        expert: true,
        officeAddress: officeAddressEl.value.trim(),
        /* 수정되었ㄷ고: 선택한 Enum 코드들을 그대로 전송 */
        fields: Array.from(selectedCodes)
    };

    // 검증
    if (!dto.name || !dto.username || !dto.password || !password2El.value || !dto.email || !dto.phone || !dto.address) {
        return alert('모든 필수 항목을 입력해주세요.');
    }
    if (!validatePasswords(passwordEl, password2El, pwdMsg, true)) return alert('비밀번호를 다시 확인해주세요.');
    if (!isUsernameChecked) return alert('아이디 중복확인을 완료해주세요.');
    if (!isEmailVerified) return alert('이메일 인증을 완료해주세요.');
    if (dto.phone.length < 9 || dto.phone.length > 11) return alert('전화번호는 숫자만 9~11자리로 입력해주세요.');
    if (!(certFile.files && certFile.files.length)) return alert('자격증명서를 첨부해주세요.');
    /* 수정되었ㄷ고: 전문분야 최소 1개 이상 선택 요구 */
    if (!dto.fields || dto.fields.length === 0) return alert('전문분야를 최소 1개 이상 선택해주세요.');
    if (dto.termsAgree !== 'Y') return alert('약관에 동의해야 가입할 수 있습니다.');

    const fd = new FormData();
    fd.append('data', new Blob([JSON.stringify(dto)], { type:'application/json' }));
    fd.append('certificationFile', certFile.files[0]);

    try {
        const res = await fetch(`${API_BASE}/signup`, { method:'POST', body:fd, headers: csrf(), credentials:'same-origin' });
        const data = await res.json();
        if (res.ok && data?.success) { alert('전문가 회원가입 요청이 접수되었습니다.'); location.href = LOGIN_PATH; }
        else { console.error(data); alert(data?.message || '회원가입 중 오류가 발생했습니다.'); }
    } catch(err){ console.error(err); alert('네트워크 오류가 발생했습니다.'); }
});
