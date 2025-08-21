// static/js/expert/expert_signup_step2.js
import {
  validatePasswords,
  setupPhoneValidation,
  checkUsername,
  setupEmailVerification,
  setupAddressSearch
} from '/js/public/common.js';

document.addEventListener('DOMContentLoaded', () => {
  // 1) 동의 체크 (Step1 저장 내용 확인)
  let consent = null;
  try {
    consent = JSON.parse(sessionStorage.getItem('expertConsent') || '{}');
  } catch (_) {}
  if (consent?.termsAgree !== 'Y') {
    alert('약관 동의 후 진행해 주세요.');
    window.location.href = '/expertTermPage'; // 전문가 약관 라우트에 맞춰 주세요
    return;
  }

  // 2) 엘리먼트
  const form         = document.getElementById('expertForm');
  const usernameEl   = document.getElementById('username');
  const passwordEl   = document.getElementById('password');
  const password2El  = document.getElementById('password2');
  const pwdMsgEl     = document.getElementById('pwdMsg');
  const nameEl       = document.getElementById('name');
  const emailEl      = document.getElementById('email');
  const emailCodeEl  = document.getElementById('emailCode');
  const emailMsgEl   = document.getElementById('emailMsg');
  const phoneEl      = document.getElementById('phone');
  const officeAddrEl = document.getElementById('officeAddress');
  const addrDetailEl = document.getElementById('addrDetail');
  const specialtyChipsWrap = document.getElementById('specialtyChips');
  const specialtiesHidden = document.getElementById('specialties');

  const btnCheckId   = document.getElementById('btnCheckId');
  const btnEmail     = document.getElementById('btnEmail');
  const btnVerify    = document.getElementById('btnVerifyEmail');
  const btnAddr      = document.getElementById('btnAddr');
  const btnSubmit    = document.getElementById('btnSubmit'); // 제출 버튼 (이 페이지에 있어야 함)
  const btnCancel    = document.getElementById('btnCancel');

  // 파일 업로드 UI
  const inputFile = document.getElementById('certificationFile');
  const drop      = document.getElementById('drop');
  const dropText  = document.getElementById('dropText');
  const btnFile   = document.getElementById('btnFile');

  // 초기 렌더링
  renderSpecialtyChips();
  
  // 3) 검증/공통 바인딩
  setupPhoneValidation(phoneEl);
  passwordEl.addEventListener('input', () => validatePasswords(passwordEl, password2El, pwdMsgEl, false));
  password2El.addEventListener('input', () => validatePasswords(passwordEl, password2El, pwdMsgEl, true));
  checkUsername(btnCheckId, usernameEl);
  setupEmailVerification(btnEmail, btnVerify, emailEl, emailCodeEl, emailMsgEl);
  setupAddressSearch(btnAddr, officeAddrEl); // 전문가: officeAddress 사용

  
  // 4) 칩으로 표시할 전문분야 옵션 (원하는 대로 수정/추가)
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
  
  // 5) 파일 드래그&드롭 + 버튼 선택
  const acceptRe = /\.(pdf|jpg|jpeg|png)$/i;
  ['dragenter','dragover'].forEach(evt =>
    drop.addEventListener(evt, e => { e.preventDefault(); drop.classList.add('drag'); })
  );
  ['dragleave','drop'].forEach(evt =>
    drop.addEventListener(evt, e => { e.preventDefault(); drop.classList.remove('drag'); })
  );
  drop.addEventListener('drop', e => {
    const f = e.dataTransfer?.files?.[0];
    if (f) {
      if (!acceptRe.test(f.name)) { alert('pdf/jpg/png만 업로드 가능합니다.'); return; }
      inputFile.files = e.dataTransfer.files;
      dropText.innerText = `선택됨: ${f.name}`;
    }
  });
  btnFile.addEventListener('click', () => inputFile.click());
  inputFile.addEventListener('change', () => {
    const f = inputFile.files?.[0];
    dropText.innerText = f ? `선택됨: ${f.name}` : '여기에 파일을 드래그하거나, 찾아보기를 눌러 선택하세요';
  });

  // 6) 취소(홈으로)
  btnCancel?.addEventListener('click', () => {
    if (confirm('회원가입을 취소하시겠습니까?')) {
      sessionStorage.removeItem('expertConsent');
      window.location.href = '/';
    }
  });

  // 7) 제출
  btnSubmit?.addEventListener('click', async (e) => {
    e.preventDefault();

    // 필드 기본 검증
    if (!validatePasswords(passwordEl, password2El, pwdMsgEl, true)) {
      password2El.reportValidity();
      return;
    }
    if (!form.reportValidity()) return;

    // 파일 필수 + 확장자 검사
    const file = inputFile?.files?.[0];
    if (!file) {
      alert('자격증명서 파일을 첨부해 주세요. (pdf/jpg/png)');
      return;
    }
    if (!acceptRe.test(file.name)) {
      alert('pdf/jpg/png만 업로드 가능합니다.');
      return;
    }

    // payload 조립 (SignupRequestDTO와 매핑)
    // Member.address NOT NULL 이므로 officeAddress를 address에도 함께 채움
    const payload = {
      isExpert: true,
      termsAgree: 'Y',
      username: usernameEl.value.trim(),
      password: passwordEl.value,          // 절대 null/empty로 보내지 않기
      name:     nameEl.value.trim(),
      email:    emailEl.value.trim(),
      phone:    phoneEl.value.trim(),
      address:  officeAddrEl.value.trim(), // Member.address
      addrDetail: addrDetailEl.value.trim(),
      officeAddress: officeAddrEl.value.trim()
      // 필요 시 officeName 필드도 여기에 추가
    };

    // FormData 구성 (@RequestPart("data") + certificationFile)
    const fd = new FormData();
    fd.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    fd.append('certificationFile', file);

    try {
      const res = await fetch('/api/members/signup', { method: 'POST', body: fd });
      const json = await res.json().catch(() => ({}));
      if (!res.ok || json.success === false) {
        throw new Error(json.message || '회원가입에 실패했습니다.');
      }
      alert('✅ 회원가입 완료!');
      sessionStorage.removeItem('expertConsent');
      window.location.href = '/loginPage';
    } catch (err) {
      console.error(err);
      alert(`회원가입 실패: ${err.message}`);
    }
  });
});
