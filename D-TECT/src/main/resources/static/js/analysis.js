(function () {
    const userId   = Number(document.body.dataset.userId || 0);

    const listEl   = document.getElementById('reportList');
    const searchEl = document.getElementById('searchInput');
    const pageInfo = document.getElementById('pageInfo');
    const prevBtn  = document.getElementById('prevBtn');
    const nextBtn  = document.getElementById('nextBtn');

    // 모달
    const viewer       = document.getElementById('viewer');
    const viewerFrame  = document.getElementById('viewerFrame');
    const viewerTitle  = document.getElementById('viewerTitle');
    const viewerClose  = document.getElementById('viewerClose');

    const PAGE_SIZE = 7;
    let all = [];       // 서버 데이터 전체
    let filtered = [];  // 검색 적용된 리스트
    let page = 1;

    const fmtDate = (s) => {
        if (!s) return '—';
        const d = new Date(s);
        if (Number.isNaN(d.getTime())) return '—';
        return d.toISOString().slice(0,10);
    };

    // 등급 컬럼에 표시
    const fmtRate = (r) => r ?? '—';

    function rowTemplate(row) {
        // 상태 점은 지금은 모두 ready로 간주 (원하면 서버에서 status 추가 가능)
        const dotClass = 'dot';

        // 미리보기/다운로드 URL이 없으면 비활성
        const canPreview  = !!row.previewUrl;
        const canDownload = !!row.downloadUrl;

        // a[download] 속성에 파일명을 세팅해두면 동일 출처일 때 브라우저가 그 이름을 사용
        const dlAttr = canDownload ? `href="${row.downloadUrl}" download="${(row.fileName || 'report.pdf').replace(/"/g, "'")}"` : '';

        return `
      <li class="row list-grid" data-id="${row.analIdx}">
        <div class="col col--dot"><span class="${dotClass}" aria-hidden="true"></span></div>
        <div class="col name" title="${row.fileName || ''}">${row.fileName || '-'}</div>
        <div class="col date">${fmtDate(row.createdAt)}</div>
        <div class="col size">${fmtRate(row.analRate)}</div>
        <div class="col actions">
          <button class="btn btn--ghost act-view" ${canPreview ? '' : 'disabled'}
                  data-url="${row.previewUrl || ''}" data-name="${row.fileName || ''}">미리보기</button>
          <a class="btn btn--accent act-download" ${canDownload ? dlAttr : 'aria-disabled="true" tabindex="-1"'}>다운로드</a>
        </div>
      </li>
    `;
    }

    function render() {
        const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
        page = Math.min(Math.max(1, page), totalPages);
        pageInfo.textContent = `${page} / ${totalPages}`;

        const slice = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
        listEl.innerHTML = slice.map(rowTemplate).join('');

        document.getElementById('emptyState').hidden = filtered.length !== 0;
        prevBtn.disabled = page <= 1;
        nextBtn.disabled = page >= totalPages;
    }

    function applyFilter() {
        const q = (searchEl.value || '').trim().toLowerCase();
        filtered = !q ? all.slice() : all.filter(x => (x.fileName || '').toLowerCase().includes(q));
        page = 1;
        render();
    }

    // 이벤트
    searchEl.addEventListener('input', applyFilter);
    prevBtn.addEventListener('click', () => { page--; render(); });
    nextBtn.addEventListener('click', () => { page++; render(); });

    listEl.addEventListener('click', (e) => {
        const btn = e.target.closest('.act-view');
        if (!btn || btn.disabled) return;

        const url  = btn.dataset.url;
        const name = btn.dataset.name || '미리보기';
        viewerTitle.textContent = name;
        viewerFrame.src = url; // /analysis/{id}/preview 가 PDF를 inline으로 내려줌
        viewer.classList.add('is-open');
        viewer.setAttribute('aria-hidden', 'false');
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

    async function load() {
        if (!userId) {
            console.warn('userId 없음');
            all = []; filtered = []; render();
            return;
        }
        try {
            const res = await fetch(`/analysis/api/user/${userId}/history`, { headers: { 'Accept': 'application/json' }});
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            all = await res.json();
            filtered = all.slice();
        } catch (e) {
            console.error('리스트 로드 실패:', e);
            all = []; filtered = [];
        }
        render();
    }

    load();
})();
