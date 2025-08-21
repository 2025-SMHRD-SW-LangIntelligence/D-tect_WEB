// static/js/public/common.js

// === CSRF ===
function getCsrfHeaders() {
  const header = document.querySelector('meta[name="_csrf_header"]')?.content;
  const token  = document.querySelector('meta[name="_csrf"]')?.content;
  return header && token ? { [header]: token } : {};
}
async function toJsonSafe(res) {
  const ct = res.headers.get('content-type') || '';
  const text = await res.text(); // 먼저 텍스트로 받아서
  if (ct.includes('application/json')) {
    try { return JSON.parse(text); } catch { /* fallthrough */ }
  }
  // JSON이 아니면 에러로 던짐(네트워크 탭에서 text 확인)
  const err = new Error(`Non-JSON response (${res.status})`);
  err.status = res.status;
  err.body = text;
  throw err;
}

// === 비밀번호 검증 ===
export function validatePasswords(passwordEl, password2El, msgEl, showMsg = true) {
  const p = passwordEl.value, q = password2El.value;
  password2El.classList.remove('ok','err');
  password2El.setCustomValidity('');
  if (showMsg) { msgEl.textContent=''; msgEl.className='msg'; }

  if (q.length === 0) return true;
  if (p === q && p.length >= 8) {
    if (showMsg) { msgEl.textContent='일치합니다.'; msgEl.className='msg ok'; }
    password2El.classList.add('ok'); return true;
  } else {
    if (showMsg) { msgEl.textContent='비밀번호가 일치하지 않습니다.'; msgEl.className='msg err'; }
    password2El.classList.add('err');
    password2El.setCustomValidity('비밀번호가 일치하지 않습니다.');
    return false;
  }
}

// === 전화번호 숫자만 ===
export function setupPhoneValidation(phoneEl) {
  phoneEl.addEventListener('input', e => {
    e.target.value = e.target.value.replace(/\D/g,'').slice(0,11);
  });
}

// === 아이디 중복확인 ===
export function checkUsername(buttonEl, usernameEl) {
  buttonEl.addEventListener('click', async () => {
    const username = usernameEl.value.trim();
    if (!username) { alert('아이디를 입력하세요.'); return; }

    try {
      const res = await fetch('/api/members/check-username', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
          ...getCsrfHeaders()
        },
        body: new URLSearchParams({ username }),
        credentials: 'same-origin'
      });
      const data = await toJsonSafe(res);
      alert(data.available ? `"${username}" 사용 가능` : `"${username}" 이미 사용중`);
      // 이벤트 통지
      usernameEl.dispatchEvent(new CustomEvent('username:checked', {
        detail: { username, available: !!data.available }
      }));
    } catch (err) {
      console.error('[check-username] ', err);
      // HTML 에러였던 본문 보여주면 디버깅에 도움
      alert('서버 오류(아이디 확인). 콘솔을 확인하세요.');
    }
  });
}

// === 이메일 인증 ===
export function setupEmailVerification(btnSend, btnVerify, emailEl, codeEl, msgEl) {
  btnSend.addEventListener('click', async () => {
    const email = emailEl.value.trim();
    if (!email) { alert('이메일을 입력하세요.'); return; }

    try {
      const res = await fetch('/api/members/send-code', {
        method: 'POST',
        headers: { 'Content-Type':'application/json', ...getCsrfHeaders() },
        body: JSON.stringify({ email }),
        credentials: 'same-origin'
      });
      const data = await toJsonSafe(res);
      if (data.success) {
        alert('메일 발송 완료');
        const row = document.getElementById('emailCodeRow');
        if (row) row.style.display = 'block';
        emailEl.dispatchEvent(new CustomEvent('email:codeSent', { detail: { email } }));
      } else {
        msgEl.innerText = '인증번호 전송 실패';
        msgEl.style.color = 'red';
      }
    } catch (err) {
      console.error('[send-code] ', err);
      msgEl.innerText = '인증번호 전송 중 오류';
      msgEl.style.color = 'red';
    }
  });

  btnVerify.addEventListener('click', async () => {
    const code = codeEl.value.trim();
    if (!code) { alert('인증번호 입력'); return; }
    try {
      const res = await fetch('/api/members/verify-code', {
        method: 'POST',
        headers: { 'Content-Type':'application/json', ...getCsrfHeaders() },
        body: JSON.stringify({ email: emailEl.value.trim(), code }),
        credentials: 'same-origin'
      });
      const data = await toJsonSafe(res);
      msgEl.innerText = data.success ? '✅ 이메일 인증 완료!' : '❌ 인증번호 불일치';
      msgEl.style.color = data.success ? 'green' : 'red';
      emailEl.dispatchEvent(new CustomEvent('email:verified', { detail: { success: !!data.success } }));
    } catch (err) {
      console.error('[verify-code] ', err);
      msgEl.innerText = '인증 확인 중 오류';
      msgEl.style.color = 'red';
    }
  });
}

// === 주소 검색 ===
export function setupAddressSearch(buttonEl, targetEl, detailId = 'addrDetail') {
  buttonEl.addEventListener('click', () => {
    new daum.Postcode({
      oncomplete: function(data) {
        const addr = data.userSelectedType==='R' ? data.roadAddress : data.jibunAddress;
        targetEl.value = addr;
        const detail = document.getElementById(detailId);
        if (detail) detail.focus();
      }
    }).open();
  });
}

// === 세션 스토리지 유틸 ===
export function saveDraft(key, obj) { sessionStorage.setItem(key, JSON.stringify(obj)); }
export function loadDraft(key) { try { return JSON.parse(sessionStorage.getItem(key) || '{}'); } catch { return {}; } }
export function clearDraft(key) { sessionStorage.removeItem(key); }
