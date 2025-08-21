// 공통 알림
function showResult(ok) {
    alert(ok ? '작업이 완료되었습니다.' : '작업이 완료되지 않았습니다.');
}

// 간단한 URL 유효성
function isValidUrl(u) {
    try { new URL(u); return true; } catch { return false; }
}

/* 1) 신조어 사전 업데이트 */
document.getElementById('newwordForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const word = document.getElementById('nwWord').value.trim();
    const mean = document.getElementById('nwMeaning').value.trim();

    const ok = !!(word && mean);
    // TODO: 서버 전송
    showResult(ok);

    if (ok) { e.target.reset(); }
});

/* 2) 사이트 등록 */
document.getElementById('siteForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const logo = document.getElementById('siteLogo').value.trim();
    const url = document.getElementById('siteUrl').value.trim();

    const ok = !!(logo && url && isValidUrl(url));
    // TODO: 서버 전송
    showResult(ok);

    if (ok) { e.target.reset(); }
});

/* 3) 부정 이용자 제재 */
document.getElementById('banForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const uid = document.getElementById('banId').value.trim();
    const reason = document.getElementById('banReason').value.trim();

    const ok = !!(uid && reason);
    // TODO: 서버 전송
    showResult(ok);

    if (ok) { e.target.reset(); }
});

/* 4) 전문가 가입 승인 테이블 */
const expertRows = document.getElementById('expertRows');
const checkAll = document.getElementById('checkAll');
const approveBtn = document.getElementById('approveBtn');
const refreshBtn = document.getElementById('refreshBtn');

// 더미 데이터 (API 대체)
let EXPERTS = [
    { id: 1, date: '2025-08-21', name: '김○○', specialty: '형사 / 사이버폭력', status: '대기' },
    { id: 2, date: '2025-08-22', name: '박○○', specialty: '가사', status: '대기' },
    { id: 3, date: '2025-08-22', name: '이○○', specialty: '민사 / 지재권', status: '대기' },
];

function statusBadge(s) {
    if (s === '승인') return '<span class="badge badge--approved">승인</span>';
    if (s === '반려') return '<span class="badge badge--rejected">반려</span>';
    return '<span class="badge badge--pending">대기</span>';
}

function renderRows() {
    expertRows.innerHTML = EXPERTS.map(e => `
    <li class="row table-grid" data-id="${e.id}">
      <label class="chk"><input type="checkbox" class="rowchk" /></label>
      <div>${e.date}</div>
      <div>${e.name}</div>
      <div>${e.specialty}</div>
      <div>${statusBadge(e.status)}</div>
      <div class="right"><button class="action-del" title="삭제">×</button></div>
    </li>
  `).join('');
    checkAll.checked = false;
}
renderRows();

// 전체선택
checkAll.addEventListener('change', (e) => {
    document.querySelectorAll('.rowchk').forEach(chk => chk.checked = e.target.checked);
});

// 행 삭제
expertRows.addEventListener('click', (e) => {
    if (!e.target.classList.contains('action-del')) return;
    const li = e.target.closest('.row');
    const id = Number(li.dataset.id);
    EXPERTS = EXPERTS.filter(x => x.id !== id);
    renderRows();
});

// 승인 처리
approveBtn.addEventListener('click', () => {
    const selected = [...document.querySelectorAll('.rowchk')].filter(chk => chk.checked);
    const ok = selected.length > 0;

    if (ok) {
        // 여기에서 실제 승인 API를 호출하면 됨
        selected.forEach(chk => {
            const id = Number(chk.closest('.row').dataset.id);
            const idx = EXPERTS.findIndex(x => x.id === id);
            if (idx >= 0) EXPERTS[idx].status = '승인';
        });
        renderRows();
    }
    showResult(ok);
});

// 새로고침(목록 재동기화)
refreshBtn.addEventListener('click', () => {
    // TODO: 서버에서 다시 조회
    renderRows();
    showResult(true);
});

/* 기타 */
document.getElementById('logoutBtn').addEventListener('click', () => alert('로그아웃 처리'));
