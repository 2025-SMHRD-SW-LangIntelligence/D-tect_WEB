let DATA = [];

const listEl   = document.getElementById('reportList');
const searchEl = document.getElementById('searchInput');
const pageInfo = document.getElementById('pageInfo');
const prevBtn  = document.getElementById('prevBtn');
const nextBtn  = document.getElementById('nextBtn');

// 모달(미리보기)
const viewer      = document.getElementById('viewer');
const viewerFrame = document.getElementById('viewerFrame');
const viewerTitle = document.getElementById('viewerTitle');
const viewerClose = document.getElementById('viewerClose');

const PAGE_SIZE = 10;
let page = 1;

// 날짜 포맷
const fmtDate = (v) => {
    if (!v) return '-';
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) return '-';
    return d.toISOString().slice(0, 10); // yyyy-MM-dd
};

// 검색 필터
function getFiltered() {
    const q = (searchEl.value || '').trim().toLowerCase();
    if (!q) return DATA;
    return DATA.filter(x => (x.fileName || '').toLowerCase().includes(q));
}

function render() {
    const rows = getFiltered();
    const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    page = Math.min(Math.max(1, page), totalPages);
    pageInfo.textContent = `${page} / ${totalPages}`;

    const slice = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

    listEl.innerHTML = slice.map(row => {
        const dotClass = 'dot';
        const created  = fmtDate(row.createdAt);
        const rate     = row.analRate ?? '-';

        return `
      <li class="row list-grid" data-id="${row.analIdx}">
        <div class="col col--dot"><span class="${dotClass}" aria-hidden="true"></span></div>
        <div class="col name" title="${row.fileName}">${row.fileName}</div>
        <div class="col date">${created}</div>
        <div class="col size">${rate}</div>
        <div class="col actions">
          <button class="btn btn--ghost act-view"
                  data-url="${row.previewUrl}"
                  data-name="${row.fileName}">보기</button>
          <a class="btn btn--accent act-download"
             href="${row.downloadUrl}"
             download="${row.fileName}">다운로드</a>
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

// 보기(미리보기) 버튼
listEl.addEventListener('click', (e) => {
    const btn = e.target.closest('.act-view');
    if (!btn) return;
    const url  = btn.dataset.url;
    const name = btn.dataset.name || '미리보기';
    viewerTitle.textContent = name;
    viewerFrame.src = url; // 서버가 inline으로 내려주므로 바로 렌더
    viewer.classList.add('is-open');
    viewer.setAttribute('aria-hidden', 'false');
});

// 모달 닫기
function closeViewer() {
    viewer.classList.remove('is-open');
    viewer.setAttribute('aria-hidden', 'true');
    viewerFrame.src = 'about:blank';
}
viewerClose.addEventListener('click', closeViewer);
viewer.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal__backdrop')) closeViewer();
});

// 서버에서 목록 불러오기
async function load() {
    const userId = Number(document.body.dataset.userId || 0);
    if (!userId) {
        console.warn('userId missing in data-user-id');
        DATA = [];
        return render();
    }
    try {
        const res = await fetch(`/analysis/api/user/${userId}/history`, {
            headers: { 'Accept': 'application/json' }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        DATA = await res.json(); // [{analIdx,fileName,createdAt,analRate,previewUrl,downloadUrl}, ...]
    } catch (e) {
        console.error('목록 로드 실패:', e);
        DATA = [];
    }
    page = 1;
    render();
}

// 초기 로드
load();
