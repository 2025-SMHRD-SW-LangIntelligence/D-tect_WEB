const form = document.getElementById('userForm');
const goTerms = document.getElementById('goTerms');

const pwd = document.getElementById('pwd');
const pwd2 = document.getElementById('pwd2');
const pwdMsg = document.getElementById('pwdMsg');
const phone = document.getElementById('phone');

const btnCheckId = document.getElementById('btnCheckId');
const btnEmail = document.getElementById('btnEmail');
const btnAddr = document.getElementById('btnAddr');

// 데모 핸들러
btnCheckId.addEventListener('click', () => {
    const id = document.getElementById('userid').value.trim();
    if (!id) return alert('아이디를 입력해 주세요.');
    alert(`아이디 "${id}" 사용 가능합니다(데모).`);
});
btnEmail.addEventListener('click', () => {
    const email = document.getElementById('email').value.trim();
    if (!email) return alert('이메일을 입력해 주세요.');
    alert(`인증 메일을 "${email}"로 발송했습니다(데모).`);
});
btnAddr.addEventListener('click', () => {
    alert('주소 검색 모달을 띄웁니다(데모).');
});

// 전화번호 정제/검증
phone.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 11);
    if (/^[0-9]{10,11}$/.test(e.target.value)) {
        phone.setCustomValidity('');
    } else {
        phone.setCustomValidity('');
    }
});

// 비밀번호 일치 확인
function validatePasswords(showMsg = true) {
    const p = pwd.value, q = pwd2.value;
    pwd2.classList.remove('ok', 'err');
    pwd2.setCustomValidity('');
    if (showMsg) { pwdMsg.textContent = ''; pwdMsg.className = 'msg'; }

    if (q.length === 0) return true;
    if (p === q && p.length >= 8) {
        if (showMsg) { pwdMsg.textContent = '일치합니다.'; pwdMsg.className = 'msg ok'; }
        pwd2.classList.add('ok'); return true;
    } else {
        if (showMsg) { pwdMsg.textContent = '비밀번호가 일치하지 않습니다.'; pwdMsg.className = 'msg err'; }
        pwd2.classList.add('err'); pwd2.setCustomValidity('비밀번호가 일치하지 않습니다.');
        return false;
    }
}
pwd.addEventListener('input', () => validatePasswords(false));
pwd2.addEventListener('input', () => validatePasswords(true));

// 약관 보기 이동
goTerms.addEventListener('click', () => {
    // 최종 정제/검증
    phone.value = phone.value.replace(/\D/g, '').slice(0, 11);
    if (!/^[0-9]{10,11}$/.test(phone.value)) {
        phone.setCustomValidity('휴대폰 번호는 숫자 10~11자리(하이픈 없이)로 입력해주세요.');
        phone.reportValidity(); return;
    } else { phone.setCustomValidity(''); }

    if (!validatePasswords(true)) { pwd2.reportValidity(); return; }
    if (!form.reportValidity()) return;

    const data = {
        name: document.getElementById('name').value.trim(),
        userid: document.getElementById('userid').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: phone.value,
        addr: document.getElementById('addr').value.trim(),
        addrDetail: document.getElementById('addrDetail').value.trim(),
    };
    sessionStorage.setItem('userSignupDraft', JSON.stringify(data));
    location.href = './user_terms.html';
});
