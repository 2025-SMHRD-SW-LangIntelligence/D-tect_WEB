// 엘리먼트
const form = document.getElementById('expertForm');
const phone = document.getElementById('phone');
const btnCheckId = document.getElementById('btnCheckId');
const btnEmail = document.getElementById('btnEmail');
const btnAddr = document.getElementById('btnAddr');
const btnFile = document.getElementById('btnFile');
const inputFile = document.getElementById('cert');
const drop = document.getElementById('drop');
const goTerms = document.getElementById('goTerms');
const specialtyChipsWrap = document.getElementById('specialtyChips');
const specialtiesHidden = document.getElementById('specialties');


// ===== 데모 핸들러(실 서비스 연동 시 교체) =====

// 아이디 중복체크(샘플)
btnCheckId.addEventListener('click', () => {
    const id = document.getElementById('userid').value.trim();
    if (!id) return alert('아이디를 입력해 주세요.');
    // TODO: 실제 중복 체크 API
    alert(`아이디 "${id}" 사용 가능합니다(데모).`);
});

// 이메일 인증(샘플)
btnEmail.addEventListener('click', () => {
    const email = document.getElementById('email').value.trim();
    if (!email) return alert('이메일을 입력해 주세요.');
    // TODO: 인증 메일 발송 API
    alert(`인증 메일을 "${email}"로 발송했습니다(데모).`);
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

// 주소 찾기(샘플)
btnAddr.addEventListener('click', () => {
    // TODO: 우편번호/주소 API(카카오, 다음 등) 연결
    alert('주소 검색 모달을 띄웁니다(데모).');
});
// 칩으로 표시할 전문분야 옵션 (원하는 대로 수정/추가)
const SPECIALTY_OPTIONS = [
    '외모·신체 비하',
    '성희롱·성적발언',
    '인신공격·모욕',
    '콘텐츠·실력비하',
    '혐오발언',
    '스팸·채팅도배'

];
function renderSpecialtyChips() {
    specialtyChipsWrap.innerHTML = '';
    SPECIALTY_OPTIONS.forEach((label, idx) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'chip selectable';
        btn.textContent = label;

        // 접근성: 체크박스 역할/상태
        btn.setAttribute('role', 'checkbox');
        btn.setAttribute('tabindex', '0');
        btn.setAttribute('aria-checked', 'false');
        btn.dataset.value = label;

        // 클릭/엔터/스페이스로 토글
        const toggle = () => {
            const selected = btn.getAttribute('aria-checked') === 'true';
            btn.setAttribute('aria-checked', String(!selected));
            btn.classList.toggle('selected', !selected);
            updateSpecialtiesHidden();
        };
        btn.addEventListener('click', toggle);
        btn.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); }
        });

        specialtyChipsWrap.appendChild(btn);
    });
}

function updateSpecialtiesHidden() {
    const selected = [...specialtyChipsWrap.querySelectorAll('.chip.selectable[aria-checked="true"]')]
        .map(el => el.dataset.value);
    specialtiesHidden.value = selected.join(',');

    // 필요 시 커스텀 유효성 처리
    specialtiesHidden.setCustomValidity(selected.length ? '' : '전문분야를 최소 1개 이상 선택해 주세요.');
}

// 초기 렌더링
renderSpecialtyChips();

// 파일 버튼
btnFile.addEventListener('click', () => inputFile.click());

// 드래그&드롭 업로드
['dragenter', 'dragover'].forEach(evt =>
    drop.addEventListener(evt, e => { e.preventDefault(); drop.classList.add('drag'); })
);
['dragleave', 'drop'].forEach(evt =>
    drop.addEventListener(evt, e => { e.preventDefault(); drop.classList.remove('drag'); })
);
drop.addEventListener('drop', (e) => {
    const files = e.dataTransfer.files;
    if (files && files[0]) {
        inputFile.files = files;
        document.getElementById('dropText').innerText = `선택됨: ${files[0].name}`;
    }
});
inputFile.addEventListener('change', () => {
    const f = inputFile.files?.[0];
    if (f) document.getElementById('dropText').innerText = `선택됨: ${f.name}`;
});

phone.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 11);
    // 입력 단계에서 에러 메시지 제거
    if (/^[0-9]{10,11}$/.test(e.target.value)) {
        phone.setCustomValidity('');
    }
});

// 제출 시 최종 검증
form.addEventListener('submit', (e) => {
    if (!/^[0-9]{10,11}$/.test(phone.value)) {
        e.preventDefault();
        phone.setCustomValidity('휴대폰 번호는 숫자 10~11자리(하이폰 없이)로 입력해주세요.');
        phone.reportValidity();
    } else {
        phone.setCustomValidity('');
    }
});

// 약관 보기로 이동 (기본 유효성 검사 + 세션 저장)
goTerms.addEventListener('click', () => {
    updateSpecialtiesHidden();
    if (!specialtiesHidden.value) {
        specialtiesHidden.reportValidity();   // 브라우저 기본 에러 풍선
        specialtyChipsWrap.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }


    if (!form.reportValidity()) return; // HTML 표준 검증 메시지

    const data = {
        name: document.getElementById('name').value.trim(),
        userid: document.getElementById('userid').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: document.getElementById('phone').value.trim(),
        addr: document.getElementById('addr').value.trim(),
        addrDetail: document.getElementById('addrDetail').value.trim(),
    };
    sessionStorage.setItem('expertSignupDraft', JSON.stringify(data));
    // TODO: 파일은 서버 업로드/프리서인 URL 로직 필요
    location.href = './expert_terms.html';
});
