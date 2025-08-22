<<<<<<< HEAD
function showResult(ok, msg) {
    alert(ok ? (msg || '작업이 완료되었습니다.') : (msg || '작업이 실패했습니다.'));
}

document.getElementById('newwordForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const word = document.getElementById('nwWord').value.trim();
    const mean = document.getElementById('nwMeaning').value.trim();
    const ok = !!(word && mean);
    // TODO: 서버 호출
    showResult(ok);
    if (ok) e.target.reset();
});

document.getElementById('siteForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const logo = document.getElementById('siteLogo').value.trim();
    const url = document.getElementById('siteUrl').value.trim();
    let ok = !!(logo && url);
    try { new URL(url); } catch { ok = false; }
    // TODO: 서버 호출
    showResult(ok);
    if (ok) e.target.reset();
});

document.getElementById('banForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const uid = document.getElementById('banId').value.trim();
    const reason = document.getElementById('banReason').value.trim();
    const ok = !!(uid && reason);
    // TODO: 서버 호출
    showResult(ok);
    if (ok) e.target.reset();
});

// ---- 전문가 승인/거절 ----
const expertRows = document.getElementById('expertRows');
const checkAll   = document.getElementById('checkAll');
const approveBtn = document.getElementById('approveBtn');
const refreshBtn = document.getElementById('refreshBtn');

const FIELD_KO = {
    VIOLENCE:  '폭력',
    DEFAMATION:'명예훼손',
    STALKING:  '스토킹',
    SEXUAL:    '성범죄',
    LEAK:      '정보유출',
    BULLYING:  '따돌림/집단괴롭힘',
    CHANTAGE:  '협박/갈취',
    EXTORTION: '공갈/강요'
};

let PENDING = [];

function renderRows() {
    if (!Array.isArray(PENDING)) PENDING = [];
    expertRows.innerHTML = PENDING.map(e => {
        const applied = e.joinedAt ? new Date(e.joinedAt).toISOString().slice(0,10) : '-';
        const fields = Array.isArray(e.fields) && e.fields.length
            ? e.fields.map(code => FIELD_KO[code] || code).join(' / ')
            : '-';

        const certBtn = e.hasCertificate
            ? `<a class="btn btn--ghost" href="/admin/experts/${e.expertIdx}/certificate">다운로드</a>`
            : `<span class="muted">없음</span>`;

        return `
      <li class="row table-grid" data-id="${e.expertIdx}">
       <label class="chk">
  <input type="checkbox" class="rowchk" ${e.hasCertificate ? '' : 'disabled title="자격증명 없음"'} />
</label>
        <div>${applied}</div>
        <div>${e.name || '-'}</div>
        <div>${fields}</div>
        <div>${certBtn}</div>
        <div class="right"><button class="action-del" title="거절">×</button></div>
      </li>
    `;
    }).join('');
    checkAll.checked = false;
}

checkAll.addEventListener('change', (e) => {
    document.querySelectorAll('.rowchk').forEach(chk => chk.checked = e.target.checked);
});

expertRows.addEventListener('click', async (e) => {
    if (!e.target.classList.contains('action-del')) return;
    const li = e.target.closest('.row');
    const id = Number(li?.dataset?.id);
    if (!id) return;

    if (!confirm('해당 전문가를 거절(삭제)하시겠습니까?')) return;

    try {
        const res = await fetch(`/admin/api/experts/${id}/reject`, { method: 'POST' });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        PENDING = PENDING.filter(x => x.expertIdx !== id);
        renderRows();
        showResult(true, '거절되었습니다.');
    } catch (err) {
        console.error(err);
        showResult(false, '거절 처리 실패');
    }
});

approveBtn.addEventListener('click', async () => {
    // ✅ 체크된 행만 추출
    const ids = [...document.querySelectorAll('.rowchk:checked')]
        .map(chk => Number(chk.closest('.row').dataset.id))
        .filter(n => Number.isFinite(n) && n > 0);

    if (ids.length === 0) {
        showResult(false, '선택된 항목이 없습니다.');
        return;
    }

    try {
        const res = await fetch('/admin/api/experts/approve', {
            method: 'POST',
            headers: { 'Content-Type':'application/json' },
            body: JSON.stringify({ ids })
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const json = await res.json(); // { ok:true, count:n }

        PENDING = PENDING.filter(x => !ids.includes(x.expertIdx));
        renderRows();
        showResult(true, `${json.count}건 승인되었습니다.`);
    } catch (err) {
        console.error(err);
        showResult(false, '승인 실패');
    }
});

refreshBtn.addEventListener('click', loadPending);

async function loadPending() {
    try {
        const res = await fetch('/admin/api/experts/pending', { headers: { 'Accept':'application/json' }});
        if (!res.ok) throw new Error('HTTP ' + res.status);
        PENDING = await res.json();
    } catch (err) {
        console.error(err);
        PENDING = [];
    }
    renderRows();
}

// 초기 로드
loadPending();

document.getElementById('logoutBtn').addEventListener('click', () => alert('로그아웃 처리'));
=======
// 공통 알림
function showResult(ok, msg) {
    alert(ok ? (msg || '작업이 완료되었습니다.') : (msg || '작업이 실패했습니다.'));
}

// ✅ CSRF 메타 읽기
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

function getCookie(name) {
    return document.cookie
        .split('; ')
        .find(v => v.startsWith(name + '='))
        ?.split('=')[1];
}

function getHiddenCsrf() {
    const input = document.querySelector('#logoutForm input[type="hidden"]');
    if (!input) return null;
    return { name: input.getAttribute('name') || '_csrf', value: input.value };
}

function getCsrfParam() {
    // 1) 템플릿 메타 태그로 받은 토큰
    if (CSRF_TOKEN) return { name: '_csrf', value: CSRF_TOKEN };
    // 2) 로그아웃 폼 hidden (Thymeleaf가 만들어 줌)
    const hidden = getHiddenCsrf();
    if (hidden?.value) return { name: hidden.name || '_csrf', value: hidden.value };
    // 3) CookieCsrfTokenRepository 쿠키
    const xsrf = getCookie('XSRF-TOKEN');
    if (xsrf) return { name: '_csrf', value: decodeURIComponent(xsrf) };
    return null;
}

(function patchGlobalFetch(){
    const orig = window.fetch;
    window.fetch = function(url, init) {
        const opts = init || {};
        const method = (opts.method || 'GET').toUpperCase();
        const needs = !['GET','HEAD','OPTIONS','TRACE'].includes(method);

        // 헤더 부착(있으면 좋고, 없어도 아래에서 쿼리파라미터로 우회함)
        if (needs) {
            const headers = new Headers(opts.headers || {});
            const metaHeader = CSRF_HEADER || 'X-CSRF-TOKEN';
            const csrf = getCsrfParam();
            if (csrf?.value) {
                headers.set(metaHeader, csrf.value);
                headers.set('X-XSRF-TOKEN', csrf.value);
                if (!headers.has('X-Requested-With')) headers.set('X-Requested-With', 'XMLHttpRequest');
            }
            opts.headers = headers;
            if (!opts.credentials) opts.credentials = 'same-origin';
        } else {
            if (!opts.credentials) opts.credentials = 'same-origin';
        }
        return orig(url, opts);
    };
})();

/* 1) 신조어 사전 업데이트 (데모) */
document.getElementById('newwordForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const word = document.getElementById('nwWord').value.trim();
    const mean = document.getElementById('nwMeaning').value.trim();
    const ok = !!(word && mean);
    showResult(ok);
    if (ok) e.target.reset();
});

/* 2) 사이트 등록 (데모) */
document.getElementById('siteForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const logo = document.getElementById('siteLogo').value.trim();
    const url  = document.getElementById('siteUrl').value.trim();
    let ok = !!(logo && url);
    try { new URL(url); } catch { ok = false; }
    showResult(ok);
    if (ok) e.target.reset();
});

/* 3) 부정 이용자 제재 (데모) */
document.getElementById('banForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const uid = document.getElementById('banId').value.trim();
    const reason = document.getElementById('banReason').value.trim();
    const ok = !!(uid && reason);
    showResult(ok);
    if (ok) e.target.reset();
});

/* 4) 전문가 승인/거절 */
const expertRows = document.getElementById('expertRows');
const checkAll   = document.getElementById('checkAll');
const approveBtn = document.getElementById('approveBtn');
const refreshBtn = document.getElementById('refreshBtn');

const FIELD_KO = {
    VIOLENCE:  '폭력',
    DEFAMATION:'명예훼손',
    STALKING:  '스토킹',
    SEXUAL:    '성범죄',
    LEAK:      '정보유출',
    BULLYING:  '따돌림/집단괴롭힘',
    CHANTAGE:  '협박/갈취',
    EXTORTION: '공갈/강요'
};

let PENDING = [];

function renderRows() {
    if (!Array.isArray(PENDING)) PENDING = [];
    expertRows.innerHTML = PENDING.map(e => {
        const applied = e.joinedAt ? new Date(e.joinedAt).toISOString().slice(0,10) : '-';
        const fields = Array.isArray(e.fields) && e.fields.length
            ? e.fields.map(code => FIELD_KO[code] || code).join(' / ')
            : '-';

        const certBtn = e.hasCertificate
            ? `<a class="btn btn--ghost" href="/admin/experts/${e.expertIdx}/certificate">다운로드</a>`
            : `<span class="muted">없음</span>`;

        return `
      <li class="row table-grid" data-id="${e.expertIdx}">
        <label class="chk"><input type="checkbox" class="rowchk" /></label>
        <div>${applied}</div>
        <div>${e.name || '-'}</div>
        <div>${fields}</div>
        <div>${certBtn}</div>
        <div class="right"><button class="action-del" title="거절">×</button></div>
      </li>
    `;
    }).join('');
    checkAll.checked = false;
}

// 전체선택
checkAll.addEventListener('change', (e) => {
    document.querySelectorAll('.rowchk').forEach(chk => chk.checked = e.target.checked);
});

// 거절
expertRows.addEventListener('click', async (e) => {
    if (!e.target.classList.contains('action-del')) return;
    const li = e.target.closest('.row');
    const id = Number(li?.dataset?.id);
    if (!id) return;

    if (!confirm('해당 전문가를 거절(삭제)하시겠습니까?')) return;

    try {
        const csrf = getCsrfParam();
        const url = csrf?.value
            ? `/admin/api/experts/${id}/reject?${encodeURIComponent(csrf.name)}=${encodeURIComponent(csrf.value)}`
            : `/admin/api/experts/${id}/reject`;
        const res = await fetch(url, { method: 'POST' });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        PENDING = PENDING.filter(x => x.expertIdx !== id);
        renderRows();
        showResult(true, '거절되었습니다.');
    } catch (err) {
        console.error(err);
        showResult(false, '거절 처리 실패');
    }
});

// 승인
approveBtn.addEventListener('click', async () => {
    const ids = [...document.querySelectorAll('.rowchk:checked')]
        .map(chk => Number(chk.closest('.row').dataset.id))
        .filter(n => Number.isFinite(n) && n > 0);

    if (ids.length === 0) {
        showResult(false, '선택된 항목이 없습니다.');
        return;
    }

    try {
        const csrf = getCsrfParam();
        const url = csrf?.value
            ? `/admin/api/experts/approve?${encodeURIComponent(csrf.name)}=${encodeURIComponent(csrf.value)}`
            : `/admin/api/experts/approve`;

        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type':'application/json' },
            body: JSON.stringify({ ids })
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const json = await res.json(); // { ok:true, count:n }

        PENDING = PENDING.filter(x => !ids.includes(x.expertIdx));
        renderRows();
        showResult(true, `${json.count}건 승인되었습니다.`);
    } catch (err) {
        console.error(err);
        showResult(false, '승인 실패');
    }
});

// 대기목록 로드
refreshBtn.addEventListener('click', loadPending);

async function loadPending() {
    try {
        const res = await fetch('/admin/api/experts/pending', { headers: { 'Accept':'application/json' } });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        PENDING = await res.json();
    } catch (err) {
        console.error(err);
        PENDING = [];
    }
    renderRows();
}

// 초기 로드
loadPending();

// 로그아웃
document.getElementById('logoutBtn').addEventListener('click', () => {
    document.getElementById('logoutForm').submit();
});
>>>>>>> branch 'dev' of https://github.com/2025-SMHRD-SW-LangIntelligence/D-tect_WEB.git
