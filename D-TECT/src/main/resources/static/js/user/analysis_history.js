// 더미 데이터 (실제 서비스에선 API 응답으로 대체)
const DATA = [
    { id: 1, name: '2025-06-28 분석결과서.pdf', url: './reports/2025-06-28.pdf', size: 1048576, createdAt: '2025-06-28T09:10:00+09:00', status: 'ready' },
    { id: 2, name: '2025-06-12 보고서.pdf', url: './reports/2025-06-12.pdf', size: 589312, createdAt: '2025-06-12T18:30:00+09:00', status: 'ready' },
    { id: 3, name: '2025-06-05 분석.pdf', url: './reports/2025-06-05.pdf', size: 392000, createdAt: '2025-06-05T14:50:00+09:00', status: 'pending' },
    { id: 4, name: '2025-05-28 결과.pdf', url: './reports/2025-05-28.pdf', size: 742000, createdAt: '2025-05-28T10:05:00+09:00', status: 'ready' },
    { id: 5, name: '2025-05-14 결과.pdf', url: './reports/2025-05-14.pdf', size: 540000, createdAt: '2025-05-14T12:12:00+09:00', status: 'error' },
    { id: 6, name: '2025-05-01 보고서.pdf', url: './reports/2025-05-01.pdf', size: 820000, createdAt: '2025-05-01T09:41:00+09:00', status: 'ready' },
    { id: 7, name: '2025-04-18 결과.pdf', url: './reports/2025-04-18.pdf', size: 470000, createdAt: '2025-04-18T11:22:00+09:00', status: 'ready' },
    { id: 8, name: '2025-04-02 보고서.pdf', url: './reports/2025-04-02.pdf', size: 390000, createdAt: '2025-04-02T16:05:00+09:00', status: 'ready' },
];

const listEl = document.getElementById('reportList');
const searchEl = document.getElementById('searchInput');
const pageInfo = document.getElementById('pageInfo');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');

// 모달
const viewer = document.getElementById('viewer');
const viewerFrame = document.getElementById('viewerFrame');
const viewerTitle = document.getElementById('viewerTitle');
const viewerClose = document.getElementById('viewerClose');

// 페이지네이션
const PAGE_SIZE = 7;
let page = 1;

// 포맷터
const fmtSize = b => {
    if (b == null) return '-';
    const units = ['B', 'KB', 'MB', 'GB'];
    let n = b, i = 0;
    while (n >= 1024 && i < units.length - 1) { n /= 1024; i++; }
    return `${n.toFixed(n < 10 && i ? 1 : 0)} ${units[i]}`;
};
const fmtDate = s => new Date(s).toISOString().slice(0, 10);

// 렌더
function getFiltered() {
    const q = (searchEl.value || '').trim().toLowerCase();
    if (!q) return DATA;
    return DATA.filter(x => x.name.toLowerCase().includes(q));
}

function render() {
    const rows = getFiltered();
    const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    page = Math.min(Math.max(1, page), totalPages);
    pageInfo.textContent = `${page} / ${totalPages}`;

    const slice = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

    listEl.innerHTML = slice.map(row => {
        const dotClass =
            row.status === 'pending' ? 'dot dot--pending' :
                row.status === 'error' ? 'dot dot--error' :
                    'dot';

        const viewDisabled = row.status !== 'ready';
        const downloadDisabled = row.status !== 'ready';

        return `
      <li class="row list-grid" data-id="${row.id}">
        <div class="col col--dot"><span class="${dotClass}" aria-hidden="true"></span></div>
        <div class="col name" title="${row.name}">${row.name}</div>
        <div class="col date">${fmtDate(row.createdAt)}</div>
        <div class="col size">${fmtSize(row.size)}</div>
        <div class="col actions">
          <button class="btn btn--ghost act-view" ${viewDisabled ? 'disabled' : ''}>보기</button>
          <a class="btn btn--accent act-download" ${downloadDisabled ? 'aria-disabled="true" tabindex="-1"' : ''} ${downloadDisabled ? '' : `href="${row.url}" download`}>
            다운로드
          </a>
        </div>
      </li>
    `;
    }).join('');

    document.getElementById('emptyState').hidden = rows.length !== 0;
}

// 이벤트
searchEl.addEventListener('input', () => { page = 1; render(); });
prevBtn.addEventListener('click', () => { page--; render(); });
nextBtn.addEventListener('click', () => { page++; render(); });

listEl.addEventListener('click', (e) => {
    const li = e.target.closest('li.row');
    if (!li) return;
    const item = DATA.find(x => String(x.id) === li.dataset.id);
    if (!item) return;

    // 보기
    if (e.target.classList.contains('act-view') && item.status === 'ready') {
        viewerTitle.textContent = item.name;
        viewerFrame.src = item.url; // 같은 출처/경로의 PDF면 곧바로 렌더됩니다.
        viewer.classList.add('is-open');
        viewer.setAttribute('aria-hidden', 'false');
    }
});

viewerClose.addEventListener('click', closeViewer);
viewer.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal__backdrop')) closeViewer();
});
function closeViewer() {
    viewer.classList.remove('is-open');
    viewer.setAttribute('aria-hidden', 'true');
    viewerFrame.src = 'about:blank';
}

// 초기 렌더
render();

/* ===== API 연동 가이드 =====
1) 서버에서 보고서 목록을 받아오면 DATA 배열을 교체한 뒤 render() 호출
   fetch('/api/reports').then(r=>r.json()).then(list => { DATA = list; render(); });

2) 각 항목은 { id, name, url, size(bytes), createdAt(ISO), status:'ready'|'pending'|'error' }
   형태를 권장.
*/
