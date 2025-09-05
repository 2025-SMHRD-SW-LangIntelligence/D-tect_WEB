(() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  // ──────────────────────────────────────────────────────────
  // analId 해석: data-anal-id → URL ?analId → window.__analId
  //            → sessionStorage → localStorage(폴백) → 경로 숫자
  // ──────────────────────────────────────────────────────────
  function pickAnalIdRaw() {
    // 1) DOM data-anal-id 우선
    const el = document.querySelector('[data-anal-id]');
    if (el?.dataset?.analId) return el.dataset.analId;

    // 2) URL 쿼리
    try {
      const urlId = new URLSearchParams(location.search).get('analId');
      if (urlId) return urlId;
    } catch {}

    // 3) 전역변수
    if (typeof window !== 'undefined' && window.__analId != null) return String(window.__analId);

    // 4) 세션 스토리지
    try {
      const saved = sessionStorage.getItem('analysisAnalId');
      if (saved) return saved;
    } catch {}

    // 4.5) 로컬 스토리지(캡처 페이지가 백업해 둠)
    try {
      const last = localStorage.getItem('analysisAnalIdLast');
      if (last) return last;
    } catch {}

    // 5) 경로 숫자 구간 (/analysis/84, /user/analysis/84/result 등)
    try {
      const m = (location.pathname || '').match(/(?:^|\/)(\d{1,20})(?:\/|$)/);
      if (m) return m[1];
    } catch {}

    return null;
  }
  function sanitizeAnalId(raw) {
    if (raw == null) return null;
    const s = String(raw).trim();
    if (s === '' || ['undefined', 'null'].includes(s.toLowerCase())) return null;
    if (!/^\d+$/.test(s)) return null;
    return s;
  }
  function getAnalId() { return sanitizeAnalId(pickAnalIdRaw()); }

  // ──────────────────────────────────────────────────────────
  // 차트 상태(스피너/메시지) 유틸
  // ──────────────────────────────────────────────────────────
  function ensureChartStatusEl() {
    const box = document.querySelector('.chart-box');
    if (!box) return null;
    let el = document.getElementById('chartStatus');
    if (!el) {
      el = document.createElement('div');
      el.id = 'chartStatus';
      el.className = 'loading-msg';
      el.innerHTML = `<div class="spinner"></div><div class="msg">분석 데이터를 불러오는 중...</div>`;
      box.appendChild(el);
    }
    return el;
  }
  function setChartStatus(type, msg) {
    const el = ensureChartStatusEl();
    if (!el) return;
    const msgEl = el.querySelector('.msg');
    if (type === 'hide') { el.remove(); return; }
    if (msgEl) msgEl.textContent = msg || '';
    el.style.display = 'block';
  }
  function renderTextFallback(labels = [], values = []) {
    const box = document.querySelector('.chart-box');
    if (!box) return;
    const wrap = document.createElement('div');
    wrap.className = 'loading-msg';
    const items = labels.map((l, i) => `<div>${l}: ${Number(values[i] ?? 0)}</div>`).join('');
    wrap.innerHTML = `<div class="msg">차트 대신 요약 값:</div>${items}`;
    box.appendChild(wrap);
  }

  // ──────────────────────────────────────────────────────────
  // API: summary / report
  // ──────────────────────────────────────────────────────────
  async function loadSummary(analId) {
    try {
      const res = await fetch(`/api/analysis/${analId}/summary`, {
        headers: { "Accept": "application/json" },
        credentials: "include"
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.warn("summary error", e);
    }
    return null;
  }

  async function fetchReportInfo(analId) {
    try {
      const res = await fetch(`/api/analysis/${analId}/report`, {
        headers: { 'Accept': 'application/json' },
        credentials: 'include'
      });
      if (res.ok) return await res.json(); // { reportUrl, name? ... }
    } catch {}
    return null;
  }

  // ──────────────────────────────────────────────────────────
  // PDF UI
  // ──────────────────────────────────────────────────────────
  function applyPdfReadyUI(reportUrl, name) {
    // (옵션) dev 버튼
    const btn = $('#btnPdf');
    if (btn) {
      btn.classList.remove('hidden');
      btn.onclick = (e) => {
        e.preventDefault();
        const a = document.createElement('a');
        a.href = reportUrl;
        a.download = '';
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        a.remove();
      };
    }
    // 타일
    const tile = $('#pdfTile');
    const caption = $('#pdfCaption');
    const spinner = $('#pdfSpinner');
    if (tile && caption) {
      tile.hidden = false;
      tile.style.pointerEvents = 'auto';
      caption.textContent = 'PDF 다운로드';
      if (spinner) spinner.style.display = 'none';
      tile.addEventListener('click', (e) => {
        e.preventDefault();
        const a = document.createElement('a');
        a.href = reportUrl;
        if (name) a.download = `${name}의 결과 보고서.pdf`;
        else a.download = '';
        document.body.appendChild(a);
        a.click();
        a.remove();
      }, { once: true });
    }
  }

  // ──────────────────────────────────────────────────────────
  // PDF 준비 알림: SSE + 폴링 백업
  // ──────────────────────────────────────────────────────────
  function listenPdfReady(analId, onReady) {
    let closed = false;
    try {
      const es = new EventSource(`/api/analysis/${analId}/events`);
      es.addEventListener('status', (ev) => {
        if (closed) return;
        try {
          const data = JSON.parse(ev.data);
          if (data.state === 'ready' && data.reportUrl) {
            closed = true;
            es.close();
            onReady(data.reportUrl, data.name || null);
          }
        } catch {}
      });
      es.onerror = () => {};
      return () => { closed = true; try { es.close(); } catch {} };
    } catch {
      return () => {};
    }
  }

  async function pollReportReady(analId, { tries = 60, intervalMs = 1000 } = {}, onReady, stopIf) {
    for (let i = 0; i < tries; i++) {
      if (stopIf()) return; // SSE가 먼저 성공한 경우 중단
      const info = await fetchReportInfo(analId);
      if (info?.reportUrl) {
        if (!stopIf()) onReady(info.reportUrl, info.name || null);
        return;
      }
      await new Promise(r => setTimeout(r, intervalMs));
    }
    // 타일 UI가 있다면 "아직 준비되지 않음" 표시
    const tile = $('#pdfTile'), caption = $('#pdfCaption'), spinner = $('#pdfSpinner');
    if (tile && caption) {
      tile.hidden = false;
      tile.style.pointerEvents = 'none';
      caption.textContent = '아직 준비되지 않음';
      if (spinner) spinner.style.display = 'none';
    }
  }

  // ──────────────────────────────────────────────────────────
  // 차트 렌더링
  // ──────────────────────────────────────────────────────────
  function normalizeTo100(values) {
    const nums = values.map(v => Number(v) || 0);
    const max = Math.max(0, ...nums);
    if (max <= 0) return nums.map(() => 0);
    return nums.map(v => Math.round((v / max) * 100));
  }

  function renderRadar({ labels, values }) {
    const canvas = $("#radar");
    if (!canvas) { setChartStatus('show', '차트 캔버스를 찾지 못했습니다.'); return; }
    if (typeof Chart === "undefined") {
      setChartStatus('show', '차트 라이브러리를 불러오지 못했습니다.');
      renderTextFallback(labels, values);
      return;
    }
    const ctx = canvas.getContext("2d");
    const normalized = normalizeTo100(values);
    new Chart(ctx, {
      type: "radar",
      data: {
        labels,
        datasets: [{
          label: "검출 비율",
          data: normalized,
          fill: true,
          backgroundColor: "rgba(239,68,68,0.18)",
          borderColor: "#ef4444",
          borderWidth: 2,
          pointBackgroundColor: "#ef4444",
          pointBorderColor: "#ef4444"
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
    setChartStatus('hide');
  }

  // ──────────────────────────────────────────────────────────
  // 초기화
  // ──────────────────────────────────────────────────────────
  (async () => {
    const analId = getAnalId();
    if (!analId) {
      console.error('[analysis_result] analId를 찾지 못했습니다. URL ?analId=, window.__analId, sessionStorage("analysisAnalId"), data-anal-id, 혹은 경로 숫자를 제공하세요.');
      const tile = $('#pdfTile'), caption = $('#pdfCaption'), spinner = $('#pdfSpinner');
      if (tile && caption) {
        tile.hidden = false;
        tile.style.pointerEvents = 'none';
        caption.textContent = '분석 ID 없음';
        if (spinner) spinner.style.display = 'none';
      }
      return;
    }

    // analId 확보에 성공했으면, 폴백 백업은 정리(선택)
    try { localStorage.removeItem('analysisAnalIdLast'); } catch {}

    // 차트: 로딩 스피너 → 성공/실패 상태
    setChartStatus('show', '분석 데이터를 불러오는 중...');
    const data = await loadSummary(analId);
    if (data) {
      if (typeof data.counts === 'string') {
        try { data.counts = JSON.parse(data.counts); } catch {}
      }
      const labels = Array.isArray(data.labels) && data.labels.length
        ? data.labels
        : (data.counts ? Object.keys(data.counts) : (data.scores ? Object.keys(data.scores) : []));
      const values = Array.isArray(data.values) && data.values.length
        ? data.values
        : (data.counts ? Object.values(data.counts) : (data.scores ? Object.values(data.scores) : []));
      if (labels?.length && values?.length) {
        renderRadar({ labels, values });
      } else {
        setChartStatus('show', '표시할 분석 데이터가 없습니다.');
        renderTextFallback(labels, values);
      }
    } else {
      setChartStatus('show', '분석 데이터를 불러오지 못했습니다.');
    }

    // PDF: 로딩 상태 먼저 노출
    let ready = false;
    const onReady = (url, name) => {
      if (ready) return;
      ready = true;
      applyPdfReadyUI(url, name);
    };
    const stopIf = () => ready;

    const tile = $('#pdfTile'), caption = $('#pdfCaption'), spinner = $('#pdfSpinner');
    if (tile && caption) {
      tile.hidden = false;
      tile.style.pointerEvents = 'none';
      caption.textContent = 'PDF 준비 중...';
      if (spinner) spinner.style.display = 'inline-block';
    }

    // SSE 우선 + 폴링 백업
    const stopSse = listenPdfReady(analId, onReady);
    pollReportReady(analId, { tries: 60, intervalMs: 1000 }, onReady, stopIf);

    // 페이지 떠날 때 SSE 닫기
    window.addEventListener('pagehide', () => { try { stopSse(); } catch {} }, { once: true });
  })();
})();
