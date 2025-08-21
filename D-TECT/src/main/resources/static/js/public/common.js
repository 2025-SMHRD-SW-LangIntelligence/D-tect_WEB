export function validatePasswords(passwordEl, password2El, msgEl, showMsg = true) {
    const p = passwordEl.value, q = password2El.value;
    password2El.classList.remove('ok', 'err');
    password2El.setCustomValidity('');
    if (showMsg) { msgEl.textContent = ''; msgEl.className = 'msg'; }

    if (q.length === 0) return true;
    if (p === q && p.length >= 8) {
        if (showMsg) { msgEl.textContent = '일치합니다.'; msgEl.className = 'msg ok'; }
        password2El.classList.add('ok');
        return true;
    } else {
        if (showMsg) { msgEl.textContent = '비밀번호가 일치하지 않습니다.'; msgEl.className = 'msg err'; }
        password2El.classList.add('err');
        password2El.setCustomValidity('비밀번호가 일치하지 않습니다.');
        return false;
    }
}

export function setupPhoneValidation(phoneEl) {
    phoneEl.addEventListener('input', e => {
        e.target.value = e.target.value.replace(/\D/g,'').slice(0,11);
    });
}

export function checkUsername(buttonEl, usernameEl) {
    buttonEl.addEventListener('click', () => {
        const username = usernameEl.value.trim();
        if (!username) { alert('아이디를 입력하세요.'); return; }
        fetch(`/api/members/check-username?username=${encodeURIComponent(username)}`, { method: 'POST' })
            .then(res => res.json())
            .then(data => {
                alert(data.available ? `"${username}" 사용 가능` : `"${username}" 이미 사용중`);
            })
            .catch(err => { console.error(err); alert('서버 오류'); });
    });
}

export function setupEmailVerification(btnSend, btnVerify, emailEl, codeEl, msgEl) {
    btnSend.addEventListener('click', () => {
        const email = emailEl.value.trim();
        if (!email) { alert('이메일을 입력하세요.'); return; }

        fetch('/api/members/send-code', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ email })
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    alert('메일 발송 완료');
                    const row = document.getElementById('emailCodeRow');
                    if (row) row.style.display = 'block';
                }
            })
            .catch(err => console.error(err));
    });

    btnVerify.addEventListener('click', () => {
        const code = codeEl.value.trim();
        if (!code) { alert('인증번호 입력'); return; }
        fetch('/api/members/verify-code', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ email: emailEl.value.trim(), code })
        })
            .then(res => res.json())
            .then(data => {
                msgEl.innerText = data.success ? "✅ 이메일 인증 완료!" : "❌ 인증번호 불일치";
                msgEl.style.color = data.success ? 'green' : 'red';
            })
            .catch(err => console.error(err));
    });
}

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

// 유틸: 세션 저장/로드
export function saveDraft(key, obj) {
    sessionStorage.setItem(key, JSON.stringify(obj));
}
export function loadDraft(key) {
    try { return JSON.parse(sessionStorage.getItem(key) || '{}'); }
    catch { return {}; }
}
export function clearDraft(key) {
    sessionStorage.removeItem(key);
}