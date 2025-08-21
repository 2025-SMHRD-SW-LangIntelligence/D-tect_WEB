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
