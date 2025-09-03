export function getCsrfHeaders() {
  const header = document.querySelector('meta[name="_csrf_header"]')?.content;
  const token  = document.querySelector('meta[name="_csrf"]')?.content;
  return header && token ? { [header]: token } : {};
}

export async function toJsonSafe(res) {
  const ct   = res.headers.get('content-type') || '';
  const text = await res.text();
  if (ct.includes('application/json')) {
    try { return JSON.parse(text); } catch {}
  }
  const err = new Error(`Non-JSON response (${res.status})`);
  err.status = res.status;
  err.body   = text;
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
      alert(data.available ? `"${username}" 사용 가능` : `"${username}" 이미 사용중"`);
      usernameEl.dispatchEvent(new CustomEvent('username:checked', {
        detail: { username, available: !!data.available }
      }));
    } catch (err) {
      console.error('[check-username]', err);
      alert('서버 오류(아이디 확인). 콘솔을 확인하세요.');
    }
  });
}

// === 이메일 인증 ===
export function setupEmailVerification(
  btnSend, btnVerify, emailEl, codeEl, msgEl,
  opts = {}
) {
  const {
    lockOnSuccess = true,          // 인증 성공 시 잠금
    lockMode = 'readonly',         // 'readonly' | 'disabled'
    resetBtn = null,               // "이메일 변경" 버튼(선택)
    codeRowId = 'emailCodeRow',    // 인증코드 행 id (선택)
  } = opts;

  const codeRowEl = document.getElementById(codeRowId);

  // 내부 유틸
  const setMsg = (text, good=false) => {
    msgEl.innerText = text || '';
    msgEl.style.color = text ? (good ? 'green' : 'red') : '';
  };

  const ensureHiddenMirror = () => {
    // disabled 잠금일 때 폼 전송을 위해 hidden 복제 필드 유지
    if (lockMode !== 'disabled') return null;
    if (!emailEl.form || !emailEl.name) return null;
    let hid = emailEl.form.querySelector(`input[type="hidden"][name="${emailEl.name}"]`);
    if (!hid) {
      hid = document.createElement('input');
      hid.type = 'hidden';
      hid.name = emailEl.name;
      emailEl.form.appendChild(hid);
    }
    hid.value = emailEl.value.trim();
    return hid;
  };

  const lockEmail = () => {
    if (!lockOnSuccess) return;
    if (lockMode === 'disabled') {
      emailEl.disabled = true;
      ensureHiddenMirror(); // hidden 업데이트
      emailEl.setAttribute('aria-disabled', 'true');
    } else {
      emailEl.readOnly = true;
      emailEl.setAttribute('aria-readonly', 'true');
    }
    emailEl.classList.add('is-verified');
    emailEl.dataset.verified = 'true';

    codeEl.disabled = true;
    btnSend.disabled = true;
    btnVerify.disabled = true;
  };

  const unlockEmail = () => {
    // 사용자가 이메일 변경을 원할 때 재인증을 위해 잠금 해제
    if (lockMode === 'disabled') {
      emailEl.disabled = false;
      emailEl.removeAttribute('aria-disabled');
      // hidden 미러는 남겨두되 값은 비움 (헷갈림 방지)
      if (emailEl.form && emailEl.name) {
        const hid = emailEl.form.querySelector(`input[type="hidden"][name="${emailEl.name}"]`);
        if (hid) hid.value = '';
      }
    } else {
      emailEl.readOnly = false;
      emailEl.removeAttribute('aria-readonly');
    }
    emailEl.classList.remove('is-verified');
    delete emailEl.dataset.verified;

    codeEl.disabled = false;
    btnSend.disabled = false;
    btnVerify.disabled = false;

    // 문구 리셋
    setMsg('');
    if (codeRowEl?.style) codeRowEl.style.display = 'block';
  };

  // "이메일 변경" 버튼 연결
  if (resetBtn) {
    const btn = typeof resetBtn === 'string' ? document.querySelector(resetBtn) : resetBtn;
    if (btn) {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        if (!confirm('이메일을 변경하시겠어요? 다시 인증이 필요합니다.')) return;
        unlockEmail();
        codeEl.value = '';
      });
    }
  }

  // 인증코드 발송
  btnSend.addEventListener('click', async () => {
    const email = emailEl.value.trim();
    if (!email) { alert('이메일을 입력하세요.'); return; }
    // 이미 인증 완료되었다면 재발송 방지
    if (emailEl.dataset.verified === 'true') {
      alert('이미 인증이 완료된 이메일입니다. 변경하려면 "이메일 변경"을 눌러주세요.');
      return;
    }
    try {
      const res = await fetch('/api/members/send-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
        body: JSON.stringify({ email }),
        credentials: 'same-origin'
      });
      const data = await toJsonSafe(res);
      if (data.success) {
        alert('메일 발송 완료');
        if (codeRowEl?.style) codeRowEl.style.display = 'block';
        emailEl.dispatchEvent(new CustomEvent('email:codeSent', { detail: { email } }));
      } else {
        setMsg('인증번호 전송 실패');
      }
    } catch (err) {
      console.error('[send-code]', err);
      setMsg('인증번호 전송 중 오류');
    }
  });

  // 인증확인
  btnVerify.addEventListener('click', async () => {
    const code = codeEl.value.trim();
    if (!code) { alert('인증번호 입력'); return; }
    try {
      const res = await fetch('/api/members/verify-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
        body: JSON.stringify({ email: emailEl.value.trim(), code }),
        credentials: 'same-origin'
      });
      const data = await toJsonSafe(res);
      const ok = !!data.success;
      setMsg(ok ? '✅ 이메일 인증 완료!' : '❌ 인증번호 불일치', ok);

      if (ok) {
        // 잠금 + hidden 미러 반영
        lockEmail();
        ensureHiddenMirror();

        // 인증 완료 이벤트(폼에서 필요 시 활용)
        emailEl.dispatchEvent(new CustomEvent('email:verified', {
          detail: { success: true, email: emailEl.value.trim() }
        }));
      } else {
        emailEl.dispatchEvent(new CustomEvent('email:verified', { detail: { success: false } }));
      }
    } catch (err) {
      console.error('[verify-code]', err);
      setMsg('인증 확인 중 오류');
    }
  });
}


// === 주소 검색 (카카오 우편번호, API로만 입력 강제 가능) ===
export function setupAddressSearch(buttonEl, targetEl, detailId = 'addrDetail', opts = {}) {
  const getEl = (x) => (typeof x === 'string' ? document.querySelector(x) : x);

  const btn      = getEl(buttonEl);
  const addrEl   = getEl(targetEl);
  const detailEl = typeof detailId === 'string' ? document.getElementById(detailId) : getEl(detailId);

  if (!btn || !addrEl) return;

  const {
    enforceApiOnly = true,           // true면 주소 본문은 카카오 API로만 입력
    lockMode       = 'readonly',     // 'readonly' | 'disabled' (disabled면 hidden 미러 생성)
    resetBtn       = null,           // "주소 변경" 버튼 선택자/엘리먼트(선택)
    meta = {},                       // 주소 메타를 채울 필드 매핑 (선택)
    // 예: meta: { zonecode:'#zonecode', sido:'#addrSido', sigungu:'#addrSigungu', bname:'#addrBname', buildingName:'#addrBldg' }
  } = opts;

  // --- 내부 유틸 ---
  const setMeta = (key, val) => {
    const sel = meta[key];
    if (!sel) return;
    const el = getEl(sel);
    if (el) { el.value = val || ''; el.setAttribute('value', el.value); }
  };

  const ensureHiddenMirror = () => {
    if (lockMode !== 'disabled') return null;
    if (!addrEl.form || !addrEl.name) return null;
    let hid = addrEl.form.querySelector(`input[type="hidden"][name="${addrEl.name}"]`);
    if (!hid) {
      hid = document.createElement('input');
      hid.type = 'hidden';
      hid.name = addrEl.name;
      addrEl.form.appendChild(hid);
    }
    hid.value = addrEl.value.trim();
    return hid;
  };

  const applyLock = () => {
    if (!enforceApiOnly) return;
    if (lockMode === 'disabled') {
      addrEl.disabled = true;
      addrEl.setAttribute('aria-disabled', 'true');
      ensureHiddenMirror();
    } else {
      addrEl.readOnly = true;
      addrEl.setAttribute('aria-readonly', 'true');
      // 사용자가 필드에 키보드/붙여넣기 시도해도 막기
      const block = (e) => e.preventDefault();
      addrEl.addEventListener('keydown', block);
      addrEl.addEventListener('paste', block);
      // 필드를 클릭해도 검색창 뜨게(UX)
      addrEl.addEventListener('click', (e) => {
        e.preventDefault();
        openPostcode();
      });
    }
    addrEl.classList.add('is-verified'); // 스타일용(선택)
  };

  const clearAddress = () => {
    addrEl.value = '';
    delete addrEl.dataset.byPostcode;
    delete addrEl.dataset.zonecode;
    delete addrEl.dataset.type;
    delete addrEl.dataset.sido;
    delete addrEl.dataset.sigungu;
    delete addrEl.dataset.bname;
    delete addrEl.dataset.buildingName;

    // 메타필드 초기화
    setMeta('zonecode',''); setMeta('sido',''); setMeta('sigungu','');
    setMeta('bname',''); setMeta('buildingName','');

    if (lockMode === 'disabled') {
      const hid = ensureHiddenMirror(); if (hid) hid.value = '';
    }
  };

  const openPostcode = () => {
    if (!window.daum || !window.daum.Postcode) {
      alert('주소 검색 모듈을 불러오지 못했습니다.');
      return;
    }
    new daum.Postcode({
      oncomplete: function(data) {
        const addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;

        addrEl.value = addr || '';
        addrEl.dataset.byPostcode   = 'true';
        addrEl.dataset.zonecode     = data.zonecode || '';
        addrEl.dataset.type         = data.userSelectedType || '';
        addrEl.dataset.sido         = data.sido || '';
        addrEl.dataset.sigungu      = data.sigungu || '';
        addrEl.dataset.bname        = data.bname || '';
        addrEl.dataset.buildingName = data.buildingName || '';

        // 메타 필드에 동기화
        setMeta('zonecode', data.zonecode);
        setMeta('sido', data.sido);
        setMeta('sigungu', data.sigungu);
        setMeta('bname', data.bname);
        setMeta('buildingName', data.buildingName);

        ensureHiddenMirror();

        // 상세주소로 포커스 이동
        if (detailEl) detailEl.focus();

        // 외부에서 활용 가능
        addrEl.dispatchEvent(new CustomEvent('address:selected', {
          bubbles: true,
          detail: { addr, data }
        }));
      }
    }).open();
  };

  // 버튼으로 열기
  btn.addEventListener('click', (e) => { e.preventDefault(); openPostcode(); });

  // "주소 변경" 버튼(선택)
  if (resetBtn) {
    const r = getEl(resetBtn);
    if (r) {
      r.addEventListener('click', (e) => {
        e.preventDefault();
        if (!confirm('주소를 변경하시겠습니까? 다시 검색해 주세요.')) return;
        clearAddress();
        // 잠금은 유지(수동 입력 방지), 검색창 바로 띄우기(선택)
        openPostcode();
      });
    }
  }

  // 초기 잠금 적용
  applyLock();
}


/* =========================
 *  프로필 수정 모달 (공통)
 * ========================= */
// - role은 /mypage/api/me 응답에서 USER/EXPERT 판별
// - 전문가: specialtyOptions 없으면 <select id="specialties"> 또는 window.__SPECIALTIES__ 사용
export function initProfileEditPopup(trigger, options = {}) {
  const getEl = (s) => (typeof s === 'string' ? document.querySelector(s) : s);
  const btn = getEl(trigger);
  if (!btn) return;

  btn.addEventListener('click', async () => {
    const res = await fetch('/mypage/api/me', { credentials: 'include' });
    if (!res.ok) {
      let msg = '내 정보를 불러오지 못했습니다.';
      try { const j = await res.json(); if (j?.message) msg = j.message; } catch {}
      alert(msg);
      if (res.status === 401) location.href = '/loginPage';
      return;
    }
    const me = await toJsonSafe(res);

    const role  = String(me.role || 'USER').toUpperCase();
    const modal = buildModal(role, options);
    document.body.appendChild(modal.root);

    modal.refs.name.value  = me.name  || '';
    modal.refs.email.value = me.email || '';

    if (role === 'USER') {
      modal.refs.address.value = me.address || '';
      setupAddressSearch(modal.refs.addrBtn, modal.refs.address);
    } else if (role === 'EXPERT') {
      modal.refs.officeName.value    = me.officeName    || '';
      modal.refs.officeAddress.value = me.officeAddress || '';
      setupAddressSearch(modal.refs.addrBtn, modal.refs.officeAddress);

      const opts = getSpecialtyOptionSource(options);
      const inputType = (options.specialtyInputType === 'radio') ? 'radio' : 'checkbox';
      buildSpecialtyGroupInputs(modal.refs.specialtyGroup, opts, me.specialtyCodes || [], inputType);
    }

    const { refs } = modal;
    const validate = () => {
      const hasCurrent = !!refs.currentPassword.value.trim();
      let ok = !!(refs.name.value.trim() && refs.email.value.trim());
      if (refs.changePwToggle.checked) {
        const lenOk   = refs.newPassword.value.length >= 8;
        const matchOk = validatePasswords(refs.newPassword, refs.newPasswordConfirm, refs.pwMsg, true);
        ok = ok && lenOk && matchOk;
      }
      refs.save.disabled = !(ok && hasCurrent);
    };

    refs.changePwToggle.addEventListener('change', () => {
      refs.changePwArea.classList.toggle('hidden', !refs.changePwToggle.checked);
      validate();
    });
    refs.form.addEventListener('input', validate);
    refs.specialtyGroup?.addEventListener('groupchange', validate);
    validate();

    // 저장
    refs.form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        name:  refs.name.value.trim(),
        email: refs.email.value.trim(),
        currentPassword: refs.currentPassword.value,
        changePassword: refs.changePwToggle.checked,
        newPassword: refs.newPassword.value || null,
        newPasswordConfirm: refs.newPasswordConfirm.value || null
      };

      if (role === 'USER') {
        payload.address = refs.address.value.trim();
      } else if (role === 'EXPERT') {
        payload.officeName    = refs.officeName.value.trim();
        payload.officeAddress = refs.officeAddress.value.trim();
        payload.specialtyCodes = getSelectedSpecialtiesFromInputs(refs.specialtyGroup);
      }

      try {
        const r = await fetch('/mypage/api/me', {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
          credentials: 'include',
          body: JSON.stringify(payload)
        });
        const updated = await toJsonSafe(r);

        const baseOpts = getSpecialtyOptionSource(options);
        defaultApplyToView(updated, role, baseOpts);

        document.dispatchEvent(new CustomEvent('profile:updated', { detail: updated }));

        alert('수정이 완료되었습니다.');
        modal.close();
      } catch (err) {
        console.error('[profile:update]', err);
        alert(err.body || err.message || '저장 중 오류가 발생했습니다.');
      }
    });
  });

  // ---- 내부 util ----
  function getSpecialtyOptionSource(opts) {
    const fromDom = readSpecialtyOptionsFromDOM();
    if (fromDom.length) return fromDom;
    if (Array.isArray(opts.specialtyOptions) && opts.specialtyOptions.length) return opts.specialtyOptions;
    if (Array.isArray(window.__SPECIALTIES__) && window.__SPECIALTIES__.length) return window.__SPECIALTIES__;
    return [];
  }
  function readSpecialtyOptionsFromDOM() {
    const sel = document.querySelector('#specialties, select[name="specialties"]');
    if (!sel) return [];
    return [...sel.options].map(o => ({ code: o.value, label: o.textContent }));
  }
  function buildSpecialtyGroupInputs(container, options, selected = [], type = 'checkbox') {
    if (!container) return;
    const sel = new Set(selected);
    const name = 'specialty';
    container.innerHTML = options.map(o => `
      <label class="pill">
        <input type="${type}" name="${name}" value="${o.code}" ${sel.has(o.code) ? 'checked' : ''}>
        <span class="pill__btn">${o.label}</span>
      </label>
    `).join('');
    container.addEventListener('change', () => {
      container.dispatchEvent(new Event('groupchange', { bubbles: true }));
    });
  }
  function getSelectedSpecialtiesFromInputs(container) {
    if (!container) return [];
    return [...container.querySelectorAll('input[type="checkbox"], input[type="radio"]')]
      .filter(i => i.checked)
      .map(i => i.value);
  }
  function defaultApplyToView(updated, role, optionsForChips = []) {
    const nameChip = document.querySelector('.user-chip .user-name, .user-chip span');
    if (nameChip) {
      nameChip.textContent = (role === 'EXPERT')
        ? (updated.name ? `${updated.name} 전문가님` : '전문가님')
        : (updated.name ? `${updated.name} 님` : '--- 님');
    }
    const setVal = (sel, v) => {
      const el = document.querySelector(sel);
      if (!el) return;
      el.value = v || '';
      el.setAttribute('value', el.value);
    };
    setVal('input[name="name"]',  updated.name);
    setVal('input[name="email"]', updated.email);

    if (role === 'USER') {
      setVal('input[name="addr"]', updated.address);
    } else if (role === 'EXPERT') {
      setVal('input[name="officeName"]',    updated.officeName);
      setVal('input[name="officeAddress"]', updated.officeAddress);

      const tagWrap = document.querySelector('.tag-wrap');
      const src = optionsForChips.length ? optionsForChips
        : (Array.isArray(window.__SPECIALTIES__) ? window.__SPECIALTIES__ : []);
      if (tagWrap && src.length) {
        const picked = new Set(updated.specialtyCodes || []);
        const chips = src
          .filter(o => picked.has(o.code))
          .map(o => `<span class="tag">${o.label}</span>`);
        tagWrap.innerHTML = chips.length ? chips.join('') : '<span class="tag tag--empty">전문분야 미등록</span>';
      }
    }
  }
  function buildModal(role) {
    const root = document.createElement('div');
    root.className = 'modal';
    root.innerHTML = `
      <div class="modal-panel">
        <button type="button" class="modal-close" data-close aria-label="닫기">&times;</button>
        <h2>내 정보 수정</h2>
        <form id="profileEditForm" class="info-form" novalidate>
          <label class="field"><span class="label">이름</span>
            <input type="text" id="name" autocomplete="name"/></label>
          <label class="field"><span class="label">이메일</span>
            <input type="email" id="email" autocomplete="email"/></label>
          ${role === 'USER' ? `
            <label class="field"><span class="label">주소</span>
              <div class="address-row">
                <input type="text" id="address" placeholder="주소를 검색하세요" autocomplete="street-address"/>
                <button type="button" id="addrSearchBtn" class="btn">주소검색</button>
              </div>
            </label>
          ` : `
            <label class="field"><span class="label">사무소명</span>
              <input type="text" id="officeName" autocomplete="organization"/></label>
            <label class="field"><span class="label">사무소 주소</span>
              <div class="address-row">
                <input type="text" id="officeAddress" placeholder="주소를 검색하세요" autocomplete="street-address"/>
                <button type="button" id="addrSearchBtn" class="btn">주소검색</button>
              </div>
            </label>
            <div class="field"><span class="label">전문분야</span>
              <div id="specialtyGroup" class="check-group" role="group" aria-label="전문분야 선택"></div>
              <small class="help">여러 개 선택 가능합니다.</small>
            </div>
          `}
          <hr/>
          <label class="field"><span class="label">현재 비밀번호 <span class="req">*</span></span>
            <input type="password" id="currentPassword" required autocomplete="current-password"/>
            <small class="help">변경 여부와 관계없이 필요합니다.</small>
          </label>
          <label class="field"><span class="label"><input type="checkbox" id="changePwToggle"/> 비밀번호 변경</span></label>
          <div id="changePwArea" class="hidden">
            <label class="field"><span class="label">새 비밀번호</span>
              <input type="password" id="newPassword" autocomplete="new-password"/></label>
            <label class="field"><span class="label">새 비밀번호 확인</span>
              <input type="password" id="newPasswordConfirm" autocomplete="new-password"/>
              <small id="pwMsg" class="msg"></small>
            </label>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn--primary" id="modalSaveBtn" disabled>저장</button>
            <button type="button" class="btn btn--ghost" data-close>취소</button>
          </div>
        </form>
      </div>
    `;

    const refs = {
      root,
      form: root.querySelector('#profileEditForm'),
      save: root.querySelector('#modalSaveBtn'),
      name: root.querySelector('#name'),
      email: root.querySelector('#email'),
      currentPassword: root.querySelector('#currentPassword'),
      changePwToggle: root.querySelector('#changePwToggle'),
      changePwArea: root.querySelector('#changePwArea'),
      newPassword: root.querySelector('#newPassword'),
      newPasswordConfirm: root.querySelector('#newPasswordConfirm'),
      pwMsg: root.querySelector('#pwMsg'),
      addrBtn: root.querySelector('#addrSearchBtn'),
      address: root.querySelector('#address'),
      officeName: root.querySelector('#officeName'),
      officeAddress: root.querySelector('#officeAddress'),
      specialtyGroup: root.querySelector('#specialtyGroup')
    };

    const close = () => { root.remove(); };
    root.addEventListener('click', (e) => {
      if (e.target.matches('[data-close]') || e.target === root) close();
    });
    document.addEventListener('keydown', escClose);
    function escClose(e){ if (e.key === 'Escape') close(); }
    const ro = new MutationObserver(() => {
      if (!document.body.contains(root)) {
        document.removeEventListener('keydown', escClose);
        ro.disconnect();
      }
    });
    ro.observe(document.body, { childList: true, subtree: true });

    return { root, refs, close };
  }
}

/* =========================
 *  로그아웃 (공통)
 * ========================= */
export async function logout(options = {}) {
  const {
    url = '/logout',
    method = 'POST',
    redirect = '/',
    includeCsrf = true,
    allowGetFallback = false,
    extraHeaders = {},
    onBefore,
    onAfter,
    successMessage = '로그아웃 되었습니다.',
  } = options;

  try {
    if (typeof onBefore === 'function') onBefore();

    const headers = {
      Accept: 'application/json,text/html;q=0.9,*/*;q=0.8',
      ...(includeCsrf ? getCsrfHeaders() : {}),
      ...extraHeaders,
    };

    const res = await fetch(url, { method, headers, credentials: 'include' });

    const finish = (targetUrl) => {
      if (successMessage) alert(successMessage);
      location.href = targetUrl ?? redirect;
    };

    if (res.status === 204) { finish(redirect); return { ok: true, status: res.status }; }
    if (res.redirected)     { finish(res.url);  return { ok: true, status: res.status }; }
    if (res.ok)             { finish(redirect); return { ok: true, status: res.status }; }

    if (res.status === 405 && allowGetFallback) {
      const r2 = await fetch(url, { method: 'GET', credentials: 'include' });
      if (r2.redirected) { finish(r2.url); return { ok: true, status: r2.status }; }
      if (r2.ok)        { finish(redirect); return { ok: true, status: r2.status }; }
      throw new Error(`Logout failed (fallback ${r2.status})`);
    }

    throw new Error(`Logout failed (${res.status})`);
  } catch (err) {
    console.error('[logout]', err);
    alert('로그아웃 중 오류가 발생했습니다.');
    return { ok: false, error: err };
  } finally {
    if (typeof onAfter === 'function') { try { onAfter(); } catch {} }
  }
}

export function setupLogout(trigger, opts = {}) {
  const getEls = (t) => {
    if (typeof t === 'string') return document.querySelectorAll(t);
    if (t instanceof Element)   return [t];
    if (t && typeof t.length === 'number') return t;
    return [];
  };
  const els = getEls(trigger);
  els.forEach(el => {
    el.addEventListener('click', async (e) => {
      e.preventDefault();
      const url      = el.dataset.logoutUrl   || opts.url      || '/logout';
      const method   = (el.dataset.logoutMethod || opts.method || 'POST').toUpperCase();
      const redirect = el.dataset.redirect    || opts.redirect || '/';

      el.disabled = true;
      el.classList.add('is-loading');
      try {
        await logout({
          url, method, redirect,
          includeCsrf: opts.includeCsrf !== false,
          allowGetFallback: !!opts.allowGetFallback,
          extraHeaders: opts.extraHeaders || {},
          onBefore: opts.onBefore,
          onAfter:  opts.onAfter,
        });
      } finally {
        el.disabled = false;
        el.classList.remove('is-loading');
      }
    });
  });
}

/* =========================
 *  회원탈퇴 (공통)
 * ========================= */
export async function withdrawAccount(payload = {}, opts = {}) {
  const {
    url = '/mypage/api/me',
    redirectFallback = '/',
    includeCsrf = true,
  } = opts;

  const headers = {
    'Content-Type': 'application/json',
    ...(includeCsrf ? getCsrfHeaders() : {}),
  };

  const res = await fetch(url, {
    method: 'DELETE',
    headers,
    credentials: 'include',
    body: JSON.stringify({
      currentPassword: payload.currentPassword ?? '',
      eraseData:       payload.eraseData      ?? true,
      reason:          payload.reason         ?? '',
    }),
  });

  let data = {};
  try { data = await toJsonSafe(res); } catch {}

  if (!res.ok) {
    const msg = (data && data.message) ? data.message : `탈퇴 실패 (HTTP ${res.status})`;
    const error = new Error(msg);
    error.status = res.status;
    error.body   = data;
    throw error;
  }

  const redirect = (data && data.redirect) ? data.redirect : redirectFallback;
  return { ok: true, redirect, data };
}

export function setupWithdraw(trigger, opts = {}) {
  const {
    confirmText  = '정말로 회원탈퇴 하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
    askEraseData = true,
    eraseDefault = true,
    askReason    = false,
    successAlert = '탈퇴가 완료되었습니다.',
    onSuccess,
    onError,
  } = opts;

  const getEls = (t) => {
    if (typeof t === 'string') return document.querySelectorAll(t);
    if (t instanceof Element)   return [t];
    if (t && typeof t.length === 'number') return t;
    return [];
  };

  getEls(trigger).forEach(el => {
    el.addEventListener('click', async (e) => {
      e.preventDefault();

      if (!confirm(confirmText)) return;

      const currentPassword = prompt('탈퇴 확인을 위해 현재 비밀번호를 입력해 주세요.');
      if (!currentPassword) return;

      let eraseData = eraseDefault;
      if (askEraseData) {
        eraseData = confirm('개인정보 및 업로드 자료까지 모두 영구 삭제할까요? (확인=삭제, 취소=보존)');
      }

      let reason = '';
      if (askReason) {
        reason = prompt('탈퇴 사유를 입력해 주세요. (선택)') ?? '';
      }

      el.disabled = true;
      el.classList.add('is-loading');
      try {
        const { redirect } = await withdrawAccount({ currentPassword, eraseData, reason });
        alert(successAlert);
        location.href = redirect || '/';
        onSuccess && onSuccess();
      } catch (err) {
        console.error('[withdraw]', err);
        alert(err.message || '탈퇴 처리 중 오류가 발생했습니다.');
        onError && onError(err);
      } finally {
        el.disabled = false;
        el.classList.remove('is-loading');
      }
    });
  });
}

/* =========================
 *  세션 스토리지 유틸 (export)
 * ========================= */
export function saveDraft(key, obj) { sessionStorage.setItem(key, JSON.stringify(obj)); }
export function loadDraft(key)     { try { return JSON.parse(sessionStorage.getItem(key) || '{}'); } catch { return {}; } }
export function clearDraft(key)    { sessionStorage.removeItem(key); }
