// static/js/public/common.js

// === CSRF ===
function getCsrfHeaders() {
	const header = document.querySelector('meta[name="_csrf_header"]')?.content;
	const token = document.querySelector('meta[name="_csrf"]')?.content;
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
	password2El.classList.remove('ok', 'err');
	password2El.setCustomValidity('');
	if (showMsg) { msgEl.textContent = ''; msgEl.className = 'msg'; }

	if (q.length === 0) return true;
	if (p === q && p.length >= 8) {
		if (showMsg) { msgEl.textContent = '일치합니다.'; msgEl.className = 'msg ok'; }
		password2El.classList.add('ok'); return true;
	} else {
		if (showMsg) { msgEl.textContent = '비밀번호가 일치하지 않습니다.'; msgEl.className = 'msg err'; }
		password2El.classList.add('err');
		password2El.setCustomValidity('비밀번호가 일치하지 않습니다.');
		return false;
	}
}

// === 전화번호 숫자만 ===
export function setupPhoneValidation(phoneEl) {
	phoneEl.addEventListener('input', e => {
		e.target.value = e.target.value.replace(/\D/g, '').slice(0, 11);
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
				headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
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
				headers: { 'Content-Type': 'application/json', ...getCsrfHeaders() },
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
		if (!window.daum || !window.daum.Postcode) {
			alert('주소 검색 모듈을 불러오지 못했습니다.');
			return;
		}
		new daum.Postcode({
			oncomplete: function(data) {
				const addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;
				targetEl.value = addr;
				const detail = document.getElementById(detailId);
				if (detail) detail.focus();
			}
		}).open();
	});
}

// ==== 공통 프로필 수정 팝업 ====
// - role은 /mypage/api/me 응답에서 USER/EXPERT 판별
// - 전문가: specialtyOptions 없으면 페이지의 <select id="specialties"> 또는 window.__SPECIALTIES__ 폴백
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

      // 전문분야 체크박스/라디오 렌더
      const opts = getSpecialtyOptionSource(options);
      const inputType = (options.specialtyInputType === 'radio') ? 'radio' : 'checkbox';
      buildSpecialtyGroupInputs(modal.refs.specialtyGroup, opts, me.specialtyCodes || [], inputType);
    }

    // 유효성 & 동작
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
        // 체크박스/라디오 선택값 수집
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

        // ✅ 저장 직후 즉시 화면 갱신 (옵션 소스 확보 → 칩 렌더)
        const baseOpts = getSpecialtyOptionSource(options);
        defaultApplyToView(updated, role, baseOpts);

        // 페이지 단에서 후처리 필요시 이벤트 발행
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
    // 1) 페이지의 <select id="specialties">가 있으면 우선
    const fromDom = readSpecialtyOptionsFromDOM();
    if (fromDom.length) return fromDom;
    // 2) init 옵션으로 전달된 값
    if (Array.isArray(opts.specialtyOptions) && opts.specialtyOptions.length) return opts.specialtyOptions;
    // 3) 서버가 심어준 전역 값
    if (Array.isArray(window.__SPECIALTIES__) && window.__SPECIALTIES__.length) return window.__SPECIALTIES__;
    return [];
  }

  function readSpecialtyOptionsFromDOM() {
    const sel = document.querySelector('#specialties, select[name="specialties"]');
    if (!sel) return [];
    return [...sel.options].map(o => ({ code: o.value, label: o.textContent }));
  }

  // 체크박스/라디오를 필 버튼 스타일로
  function buildSpecialtyGroupInputs(container, options, selected = [], type = 'checkbox') {
    if (!container) return;
    const sel  = new Set(selected);
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

      // ✅ 칩 즉시 재렌더
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

  function buildModal(role, options) {
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
            <label class="field"><span class="label">전문분야</span>
              <div id="specialtyGroup" class="check-group" role="group" aria-label="전문분야 선택"></div>
              <small class="help">여러 개 선택 가능합니다.</small>
            </label>
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

// === 세션 스토리지 유틸 ===
export function saveDraft(key, obj) { sessionStorage.setItem(key, JSON.stringify(obj)); }
export function loadDraft(key) { try { return JSON.parse(sessionStorage.getItem(key) || '{}'); } catch { return {}; } }
export function clearDraft(key) { sessionStorage.removeItem(key); }
