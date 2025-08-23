import {
  validatePasswords, setupPhoneValidation, checkUsername,
  setupEmailVerification, setupAddressSearch, loadDraft
} from '/js/public/common.js';

const API_BASE = '/api/members';
const LOGIN_PATH = '/loginpage';

function csrf(){
  const h = document.querySelector('meta[name="_csrf_header"]')?.content;
  const t = document.querySelector('meta[name="_csrf"]')?.content;
  return h && t ? { [h]: t } : {};
}

const form = document.getElementById('signupForm');
const nameEl = document.getElementById('name');
const uidEl  = document.getElementById('uid');
const pwEl   = document.getElementById('pw');
const pw2El  = document.getElementById('pw2');
const emailEl= document.getElementById('email');
const phoneEl= document.getElementById('phone');
const addrEl = document.getElementById('addr');
const addr2El= document.getElementById('addr2');

const checkIdBtn = document.getElementById('checkIdBtn');
const verifyEmailBtn = document.getElementById('verifyEmailBtn');
const btnVerifyEmail = document.getElementById('btnVerifyEmail');
const emailCodeEl = document.getElementById('emailCode');
const emailMsg = document.getElementById('emailMsg');
const addrBtn = document.getElementById('addrBtn');
const pwdMsg = document.getElementById('pwdMsg');

document.getElementById('logoLink')?.addEventListener('click',(e)=>{e.preventDefault();location.href=LOGIN_PATH;});

// 공통
setupPhoneValidation(phoneEl);
pwEl.addEventListener('input',  ()=>validatePasswords(pwEl, pw2El, pwdMsg, true));
pw2El.addEventListener('input', ()=>validatePasswords(pwEl, pw2El, pwdMsg, true));
setupAddressSearch(addrBtn, addrEl, 'addr2');

// step1 약관 복원
const draft = loadDraft('signup:step1');
let termsAgree = draft?.termsAgree === 'Y' ? 'Y' : 'N';

// 상태
let isUsernameChecked=false, isEmailVerified=false;

// 아이디/이메일
checkUsername(checkIdBtn, uidEl);
uidEl.addEventListener('username:checked', (e)=>{ isUsernameChecked = !!e.detail?.available; });

setupEmailVerification(verifyEmailBtn, btnVerifyEmail, emailEl, emailCodeEl, emailMsg);
emailEl.addEventListener('email:verified', (e)=>{ isEmailVerified = !!e.detail?.success; });

// 제출
form.addEventListener('submit', async (e)=>{
  e.preventDefault();

  const dto = {
    username: uidEl.value.trim(),
    password: (pwEl.value||'').trim(),
    name: nameEl.value.trim(),
    phone: phoneEl.value.trim(),
    email: emailEl.value.trim(),
    address: addrEl.value.trim(),
    addrDetail: addr2El.value.trim(),
    termsAgree,
    expert: false
  };

  if (!dto.name || !dto.username || !dto.password || !pw2El.value || !dto.email || !dto.phone || !dto.address)
    return alert('모든 필수 항목을 입력해주세요.');
  if (!validatePasswords(pwEl, pw2El, {textContent:'',className:'msg'}, true))
    return alert('비밀번호를 다시 확인해주세요.');
  if (!isUsernameChecked) return alert('아이디 중복확인을 완료해주세요.');
  if (!isEmailVerified)  return alert('이메일 인증을 완료해주세요.');
  if (dto.phone.length < 9 || dto.phone.length > 11)
    return alert('전화번호는 숫자만 9~11자리로 입력해주세요.');
  if (dto.termsAgree !== 'Y')
    return alert('약관에 동의해야 가입할 수 있습니다.\n(이전 단계에서 동의해주세요)');

  const fd = new FormData();
  fd.append('data', new Blob([JSON.stringify(dto)], { type:'application/json' }));

  try {
    const res = await fetch(`${API_BASE}/signup`, { method:'POST', body:fd, headers: csrf(), credentials:'same-origin' });
    const data = await res.json();
    if (res.ok && data?.success) { alert('회원가입이 완료되었습니다. 로그인해주세요.'); location.href = LOGIN_PATH; }
    else { console.error(data); alert(data?.message || '회원가입 중 오류가 발생했습니다.'); }
  } catch(err){ console.error(err); alert('네트워크 오류가 발생했습니다.'); }
});
