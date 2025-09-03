(() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  // ---- analId 해석 ----
  function pickAnalIdRaw() {
    try {
      const urlId = new URLSearchParams(location.search).get('analId');
      if (urlId) return urlId;
    } catch {}
    if (typeof window !== 'undefined' && window.__analId != null) return String(window.__analId);
    try {
      const saved = sessionStorage.getItem('analysisAnalId');
      if (saved) return saved;
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
  function getAnalId() {
    return sanitizeAnalId(pickAnalIdRaw());
  }

  // ---- summary API 호출 ----
  async function loadAnalysisData(analId) {
    if (analId) {
      try {
        const url = `/api/analysis/${encodeURIComponent(String(analId))}/summary`;
        const res = await fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'include' });
        if (res.ok) {
          const json = await res.json();
          if (json && Array.isArray(json.labels) && Array.isArray(json.values)) {
            return json;
          }
        }
      } catch (e) {
        console.warn('[analysis] summary error:', e);
      }
    }
    return null;
  }

  // ---- SSE로 PDF 준비 알림 ----
  function listenPdfReady(analId) {
    const pdfBtn = $('#btnPdf');
    if (!pdfBtn) return;

    try {
      const evtSrc = new EventSource(`/api/analysis/${analId}/events`);
      evtSrc.addEventListener('status', (ev) => {
        const data = JSON.parse(ev.data);
        if (data.state === 'ready' && data.reportUrl) {
          pdfBtn.classList.remove('hidden'); // 🔹 버튼 표시
          pdfBtn.addEventListener('click', () => {
            const a = document.createElement('a');
            a.href = data.reportUrl;
            a.download = '';
            a.target = '_blank';
            document.body.appendChild(a);
            a.click();
            a.remove();
          });
          evtSrc.close();
        }
      });
    } catch (e) {
      console.warn('[analysis] SSE 연결 실패:', e);
    }
  }

  // ---- 차트 렌더링 ----
  function normalizeTo100(values) {
    const nums = values.map(v => Number(v) || 0);
    const max = Math.max(0, ...nums);
    if (max <= 0) return nums.map(() => 0);
    return nums.map(v => Math.round((v / max) * 100));
  }

  let radarChart = null;
  function renderRadar({ labels, values, normalized }) {
    const canvas = $('#radar');
    if (!canvas || typeof Chart === 'undefined') return;
    const ctx = canvas.getContext('2d');
    if (radarChart) radarChart.destroy();

    radarChart = new Chart(ctx, {
      type: 'radar',
      data: {
        labels,
        datasets: [{
          label: '정규화(최대=100)',
          data: normalized,
          fill: true,
          backgroundColor: 'rgba(239, 68, 68, 0.18)',
          borderColor: '#ef4444',
          borderWidth: 2,
          pointBackgroundColor: '#ef4444',
          pointBorderColor: '#ef4444'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          r: {
            beginAtZero: true,
            suggestedMax: 100,
            grid: { color: 'rgba(0,0,0,.08)' },
            angleLines: { color: 'rgba(0,0,0,.08)' },
            pointLabels: { color: '#333', font: { size: 12, weight: '600' } },
            ticks: { display: false }
          }
        }
      }
    });
  }

  // ---- 초기화 ----
  (async () => {
    const analId = getAnalId();
    if (!analId) return;

    const data = await loadAnalysisData(analId);
    if (data && data.labels?.length) {
      const labels = data.labels;
      const values = data.values;
      const normalized = normalizeTo100(values);
      renderRadar({ labels, values, normalized });
    }

    // SSE로 PDF 버튼 자동 활성화
    listenPdfReady(analId);
  })();
})();
