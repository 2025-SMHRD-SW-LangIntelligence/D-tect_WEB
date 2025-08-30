/* D-tect 분석 결과 페이지 스크립트 (analId 기준 & 최댓값=100 정규화) */
(() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  // ---------------------------
  // analId 해석 (우선순위: URL ?analId → SSR window.__analId → sessionStorage)
  // ---------------------------
  function getAnalId() {
    try {
      const urlId = new URLSearchParams(location.search).get('analId');
      if (urlId) return urlId;
    } catch {}
    if (typeof window !== 'undefined' && window.__analId) return String(window.__analId);
    const saved = sessionStorage.getItem('analysisAnalId');
    return saved || null;
  }

  // ---------------------------
  // 데이터 로딩: /api/analysis/{analId}/summary → {labels:[], values:[]}
  // (실패 시 localStorage 캐시 → 샘플)
  // ---------------------------
  async function loadAnalysisData(analId) {
    if (analId) {
      try {
        const res = await fetch(`/api/analysis/${encodeURIComponent(analId)}/summary`, {
          headers: { 'Accept': 'application/json' },
          credentials: 'include'
        });
        if (res.ok) {
          const json = await res.json();
          if (json && Array.isArray(json.labels) && Array.isArray(json.values)) {
            // 캐시 (최근 결과)
            try { localStorage.setItem('analysis:last', JSON.stringify(json)); } catch {}
            return json;
          }
        }
      } catch {}
    }

    // 캐시 폴백
    try {
      const cached = localStorage.getItem('analysis:last');
      if (cached) {
        const parsed = JSON.parse(cached);
        if (parsed && Array.isArray(parsed.labels) && Array.isArray(parsed.values)) return parsed;
      }
    } catch {}

    // 샘플 폴백
    return {
      title: '샘플 결과',
      labels: ['폭력', '명예훼손', '성희롱', '따돌림', '협박', '공갈'],
      values: [22, 18, 12, 8, 26, 14]
    };
  }

  // ---------------------------
  // 정규화: 최댓값을 100으로 스케일
  // ---------------------------
  function normalizeTo100(values) {
    const nums = values.map(v => Number(v) || 0);
    const max = Math.max(0, ...nums);
    if (max <= 0) return nums.map(() => 0);
    return nums.map(v => Math.round((v / max) * 100));
  }

  // ---------------------------
  // 차트 렌더 (Chart.js Radar)
  // ---------------------------
  let radarChart = null;
  function renderRadar({ labels, values, normalized }) {
    const canvas = $('#radar');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    if (radarChart) {
      radarChart.destroy();
      radarChart = null;
    }

    radarChart = new Chart(ctx, {
      type: 'radar',
      data: {
        labels,
        datasets: [
          {
            label: '정규화(최대=100)',
            data: normalized,
            fill: true,
            backgroundColor: 'rgba(239, 68, 68, 0.18)',
            borderColor: '#ef4444',
            borderWidth: 2,
            pointBackgroundColor: '#ef4444',
            pointBorderColor: '#ef4444'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              // 툴팁에 "정규화값% (원시 n건)" 같이 표기
              label: (ctx) => {
                const i = ctx.dataIndex;
                const norm = normalized[i] ?? 0;
                const raw  = values[i] ?? 0;
                return ` ${ctx.label}: ${norm}% (원시 ${raw}건)`;
              }
            }
          }
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

  // ---------------------------
  // PDF 저장 (선택)
  // ---------------------------
  async function downloadPDF(analId, labels, values, normalized) {
    const { jsPDF } = window.jspdf || {};
    if (!jsPDF) return alert('PDF 생성 모듈을 불러올 수 없습니다.');

    const pdf = new jsPDF({ unit: 'pt', format: 'a4' });
    const margin = 40;

    pdf.setFont('helvetica', 'bold');
    pdf.setFontSize(18);
    pdf.text('D-tect 분석 결과', margin, 50);

    const canvas = $('#radar');
    const img = canvas.toDataURL('image/png', 1.0);
    const pageW = pdf.internal.pageSize.getWidth();
    const imgW = pageW - margin * 2;
    const imgH = (canvas.height / canvas.width) * imgW;

    pdf.addImage(img, 'PNG', margin, 80, imgW, imgH);

    // 원시/정규화 표기
    pdf.setFont('helvetica', 'normal');
    pdf.setFontSize(12);
    let y = 100 + imgH + 16;
    pdf.text('세부 수치 (정규화% / 원시 건수)', margin, y);
    y += 10;

    labels.forEach((label, i) => {
      y += 18;
      const norm = normalized[i] ?? 0;
      const raw  = values[i] ?? 0;
      pdf.text(`• ${label} : ${norm}%  (원시 ${raw}건)`, margin, y);
    });

    const name = analId ? `analysis_result_${analId}.pdf` : 'analysis_result.pdf';
    pdf.save(name);
  }

  // ---------------------------
  // Bootstrap
  // ---------------------------
  (async () => {
    const analId = getAnalId();

    // UI에 표시할 수 있으면 표기
    const analIdEl = $('#analIdText');
    if (analIdEl && analId) analIdEl.textContent = `#${analId}`;

    const data = await loadAnalysisData(analId);
    const labels = data.labels ?? [];
    const values = data.values ?? [];
    const normalized = normalizeTo100(values);

    // 타이틀 텍스트 업데이트(있을 경우)
    const titleEl = $('#resultTitle');
    if (titleEl) titleEl.textContent = data.title || '분석 결과';

    renderRadar({ labels, values, normalized });

    // PDF 버튼 연결(있을 때만)
    const pdfBtn = $('#btnPdf');
    if (pdfBtn) {
      pdfBtn.addEventListener('click', () => downloadPDF(analId, labels, values, normalized));
    }
  })();
})();
