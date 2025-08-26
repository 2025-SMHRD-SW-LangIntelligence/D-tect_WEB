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
	
	const sid = sessionStorage.getItem('analysisSid');
	const progressEl = document.getElementById('progress');
	const panelLoading = document.getElementById('panelLoading');
	const panelResult  = document.getElementById('panelResult');
	
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
	
	if (!sid) { progressEl.textContent = '—'; return; }

	  async function pollStatus(){
	    const res = await fetch(`/api/analysis/status?sid=${encodeURIComponent(sid)}`, { headers: { 'Accept':'application/json' }});
	    if (!res.ok) throw new Error(`HTTP ${res.status}`);
	    return res.json(); // { received, processed, total }
	  }
	  async function fetchResult(){
	    const res = await fetch(`/api/analysis/result?sid=${encodeURIComponent(sid)}`, { headers: { 'Accept':'application/json' }});
	    if (!res.ok) throw new Error(`HTTP ${res.status}`);
	    return res.json(); // [{ user, text, score, classification:{label,count} }, ...]
	  }

	  function toRadarData(arr){
	    const labelCounts = {};
	    let total = 0;
	    for (const it of arr){
	      const c = it?.classification;
	      const label = c?.label || 'UNKNOWN';
	      const cnt = Number(c?.count || 0);
	      labelCounts[label] = (labelCounts[label] || 0) + cnt;
	      total += cnt;
	    }
	    const labels = Object.keys(labelCounts);
	    const values = labels.map(l => total ? Math.round(labelCounts[l] / total * 100) : 0);
	    return { labels, values };
	  }

	  (async function loop(){
	    let percent = 0;
	    // 간단 폴링(1s)
	    for (let i=0; i<60; i++){
	      try{
	        const st = await pollStatus();
	        const denom = (st.total ?? Math.max(st.received, 1));
	        percent = Math.min(100, Math.floor((st.processed / denom) * 100));
	        progressEl.textContent = `${percent}%`;
	      }catch{ /* 네트워크 일시 오류는 무시 */ }
	      await new Promise(r => setTimeout(r, 1000));
	    }

	    // 결과 가져와 렌더
	    const arr = await fetchResult();
	    const { labels, values } = toRadarData(arr);

	    panelLoading.classList.add('hidden');
	    panelResult.classList.remove('hidden');
	    // 🔽 기존 레이더 렌더러 호출 (있다고 가정)
	    if (typeof window.renderRadar === 'function') {
	      window.renderRadar('radar', labels, values);
	    }
	  })();
	
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
