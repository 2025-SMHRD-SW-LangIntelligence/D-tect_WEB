function showResult(ok, msg) {
    alert(ok ? (msg || '작업이 완료되었습니다.') : (msg || '작업이 실패했습니다.'));
}

// CSRF 유틸
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

function getCookie(name) {
    return document.cookie.split('; ').find(v => v.startsWith(name + '='))?.split('=')[1];
}
function getHiddenCsrf() {
    const input = document.querySelector('#logoutForm input[type="hidden"]');
    if (!input) return null;
    return { name: input.getAttribute('name') || '_csrf', value: input.value };
}
function getCsrfParam() {
    if (CSRF_TOKEN) return { name: '_csrf', value: CSRF_TOKEN };
    const hidden = getHiddenCsrf();
    if (hidden?.value) return { name: hidden.name || '_csrf', value: hidden.value };
    const xsrf = getCookie('XSRF-TOKEN');
    if (xsrf) return { name: '_csrf', value: decodeURIComponent(xsrf) };
    return null;
}

// fetch 래핑(자격 증명 포함)
(function patchGlobalFetch(){
    const orig = window.fetch;
    window.fetch = function(url, init) {
        const opts = init || {};
        const method = (opts.method || 'GET').toUpperCase();
        const needs = !['GET','HEAD','OPTIONS','TRACE'].includes(method);
        if (!opts.credentials) opts.credentials = 'same-origin';
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
        }
        return orig(url, opts);
    };
})();

// ====== 좌측 카드 (데모) ======
document.getElementById('newwordForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const word = document.getElementById('nwWord').value.trim();
    const mean = document.getElementById('nwMeaning').value.trim();
    showResult(!!(word && mean));
    if (word && mean) e.target.reset();
});
document.getElementById('banForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const uid = document.getElementById('banId').value.trim();
    const reason = document.getElementById('banReason').value.trim();
    showResult(!!(uid && reason));
    if (uid && reason) e.target.reset();
});

// ====== 전문가 승인/거절 영역 ======
const expertRows  = document.getElementById('expertRows');
const pagerEl     = document.getElementById('expertPager');
const checkAll    = document.getElementById('checkAll');
const approveBtn  = document.getElementById('approveBtn');
const rejectBtn   = document.getElementById('rejectBtn');
const refreshBtn  = document.getElementById('refreshBtn');

const FIELD_KO = {
    VIOLENCE:  '폭력',
    DEFAMATION:'명예훼손',
    STALKING:  '스토킹',
    SEXUAL:    '성범죄',
    LEAK:      '정보유출',
    BULLYING:  '따돌림/집단괴롭힘',
    CHANTAGE:  '협박/갈취',
    EXTORTION: '공갈/갈취'
};

// 데이터 + 페이징 상태
let PENDING = [];
const PAGE_SIZE = 4;   // 한 페이지 최대 4건
let currentPage = 1;

function totalPages() {
    return Math.max(1, Math.ceil(PENDING.length / PAGE_SIZE));
}
function pageSlice(page) {
    const start = (page - 1) * PAGE_SIZE;
    return PENDING.slice(start, start + PAGE_SIZE);
}

// 서버 응답 표준화
function normalizeExpert(row) {
    const id        = row.expertIdx ?? row.id ?? row.expert_id ?? null;
    const name      = row.name ?? row.memberName ?? row.expertName ?? '-';
    const joinedAt  = row.joinedAt ?? row.createdAt ?? row.appliedAt ?? null;
    const fields    = row.fields ?? row.fieldNames ?? row.field_list ?? [];
    let hasCertificate;
    if (typeof row.hasCertificate !== 'undefined') hasCertificate = !!row.hasCertificate;
    else if (typeof row.hasCert !== 'undefined')   hasCertificate = !!row.hasCert;
    else hasCertificate = !!(row.certificationFile || (row.expertEncoding && row.expertVector));
    return { expertIdx: id, name, joinedAt, fields, hasCertificate };
}

// 행 렌더
function renderRows() {
    const rows = pageSlice(currentPage);

    expertRows.innerHTML = rows.map(e => {
        const applied    = e.joinedAt ? new Date(e.joinedAt).toISOString().slice(0,10) : '-';

        // 토큰 단위 줄바꿈: 개별 항목은 끊지 않고 콤마에서만 줄바꿈
        const fieldsHtml = (Array.isArray(e.fields) && e.fields.length)
            ? e.fields.map(code => {
                const label = FIELD_KO[code] || code;
                return `<span class="token">${label}</span>`;
            }).join(', ')
            : '-';

        const certBtn = e.hasCertificate
            ? `<a class="btn btn--ghost" href="/admin/experts/${e.expertIdx}/certificate">다운로드</a>`
            : `<span class="muted">없음</span>`;

        return `
      <li class="row table-grid" data-id="${e.expertIdx}">
        <label class="chk"><input type="checkbox" class="rowchk" /></label>
        <div>${applied}</div>
        <div>${e.name || '-'}</div>
        <div class="fields clamp-3">${fieldsHtml}</div>
        <div>${certBtn}</div>
      </li>
    `;
    }).join('');

    // 현재 페이지 내에서만 전체선택 적용
    checkAll.checked = false;
    renderPager();
}

function renderPager() {
    const tp = totalPages();
    let html = '';

    const prevDisabled = currentPage <= 1 ? 'disabled' : '';
    const nextDisabled = currentPage >= tp ? 'disabled' : '';

    html += `<button class="pg-btn" data-go="prev" ${prevDisabled}>이전</button>`;

    // 페이지 숫자 (현재 중심 ±2)
    const pages = [];
    const start = Math.max(1, currentPage - 2);
    const end   = Math.min(tp, currentPage + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    for (const p of pages) {
        html += `<button class="pg-btn ${p===currentPage?'is-active':''}" data-page="${p}">${p}</button>`;
    }

    html += `<button class="pg-btn" data-go="next" ${nextDisabled}>다음</button>`;
    pagerEl.innerHTML = html;

    pagerEl.querySelectorAll('.pg-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const go = btn.getAttribute('data-go');
            if (go === 'prev' && currentPage > 1) currentPage--;
            else if (go === 'next' && currentPage < totalPages()) currentPage++;
            const page = btn.getAttribute('data-page');
            if (page) currentPage = Number(page);
            renderRows();
        });
    });
}

// 전체선택(보이는 페이지 한정)
checkAll.addEventListener('change', (e) => {
    expertRows.querySelectorAll('.rowchk').forEach(chk => chk.checked = e.target.checked);
});

// 승인(보이는 페이지에서 체크된 항목만)
approveBtn.addEventListener('click', async () => {
    const ids = [...expertRows.querySelectorAll('.rowchk:checked')]
        .map(chk => Number(chk.closest('.row').dataset.id))
        .filter(n => Number.isFinite(n) && n > 0);

    if (ids.length === 0) return showResult(false, '선택된 항목이 없습니다.');

    try {
        const csrf = getCsrfParam();
        const url  = csrf?.value
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
        const tp = totalPages();
        if (currentPage > tp) currentPage = tp;
        renderRows();
        showResult(true, `${json.count}건 승인되었습니다.`);
    } catch (err) {
        console.error(err);
        showResult(false, '승인 실패');
    }
});

// 가입거절(보이는 페이지에서 체크된 항목만, 1건씩 호출)
rejectBtn.addEventListener('click', async () => {
    const ids = [...expertRows.querySelectorAll('.rowchk:checked')]
        .map(chk => Number(chk.closest('.row').dataset.id))
        .filter(n => Number.isFinite(n) && n > 0);

    if (ids.length === 0) return showResult(false, '선택된 항목이 없습니다.');
    if (!confirm('선택한 전문가들을 거절(삭제)하시겠습니까?')) return;

    try {
        let okCnt = 0;
        await Promise.allSettled(ids.map(async (id) => {
            const csrf = getCsrfParam();
            const url  = csrf?.value
                ? `/admin/api/experts/${id}/reject?${encodeURIComponent(csrf.name)}=${encodeURIComponent(csrf.value)}`
                : `/admin/api/experts/${id}/reject`;
            const res = await fetch(url, { method:'POST' });
            if (res.ok) okCnt++;
        }));

        PENDING = PENDING.filter(x => !ids.includes(x.expertIdx));
        const tp = totalPages();
        if (currentPage > tp) currentPage = tp;
        renderRows();
        showResult(true, `${okCnt}건 거절되었습니다.`);
    } catch (err) {
        console.error(err);
        showResult(false, '거절 처리 실패');
    }
});

// 대기목록 로드
const API_PENDING = '/admin/api/experts/pending';
async function loadPending() {
    try {
        const res = await fetch(API_PENDING, { headers: { 'Accept':'application/json' } });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const payload = await res.json();

        const list = Array.isArray(payload)
            ? payload
            : (Array.isArray(payload?.data) ? payload.data
                : Array.isArray(payload?.content) ? payload.content
                    : []);

        PENDING = list.map(normalizeExpert);
        currentPage = 1; // 새로고침 시 1페이지로
    } catch (err) {
        console.error('[pending] 로드 실패:', err);
        PENDING = [];
    }
    renderRows();
}

refreshBtn.addEventListener('click', loadPending);

// 초기 로드
document.addEventListener('DOMContentLoaded', loadPending);

// 로그아웃
document.getElementById('logoutBtn').addEventListener('click', () => {
    document.getElementById('logoutForm').submit();
});
