/* D-tect 분석 결과 페이지 스크립트 */
(() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  // 1) 데이터 로딩
  async function loadAnalysisData() {
    if (window.__analysisData) return window.__analysisData;

    try {
      const res = await fetch('/api/analysis/latest', { credentials: 'include' });
      if (res.ok) {
        const json = await res.json();
        if (json && json.labels && json.values) return json;
      }
    } catch {}

    try {
      const cached = localStorage.getItem('analysis:last');
      if (cached) {
        const parsed = JSON.parse(cached);
        if (parsed && parsed.labels && parsed.values) return parsed;
      }
    } catch {}

    // Fallback 샘플
    return {
      title: '샘플 결과',
      labels: ['성희롱·성적발언', '외모·신체 비하', '컨텐츠·실력비하', '혐오발언', '인신공격·모욕', '스팸·채팅도배'],
      values: [22, 18, 12, 8, 26, 14],
    };
  }

  // 2) 차트 렌더
  let radarChart = null;
  function renderRadar(data) {
    const ctx = $('#radar').getContext('2d');
    const maxAxis = 100;

    // 기존 차트 파기(중복 방지)
    if (radarChart) {
      radarChart.destroy();
      radarChart = null;
    }

    radarChart = new Chart(ctx, {
      type: 'radar',
      data: {
        labels: data.labels,
        datasets: [
          {
            label: '검출 비율(%)',
            data: data.values,
            fill: true,
            backgroundColor: 'rgba(239, 68, 68, 0.18)',
            borderColor: '#ef4444',
            borderWidth: 2,
            pointBackgroundColor: '#ef4444',
            pointBorderColor: '#ef4444',
          },
          {
            label: '기준축',
            data: Array(data.labels.length).fill(maxAxis),
            fill: false,
            borderColor: '#cfcfcf',
            borderWidth: 1,
            pointRadius: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,      // 🔸 chart-box의 높이를 그대로 사용
        resizeDelay: 100,                // 🔸 리사이즈 스로틀(무한 리플로우 예방)
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: c => `${c.dataset.label}: ${c.parsed.r} %`
            }
          }
        },
        scales: {
          r: {
            beginAtZero: true,
            suggestedMax: maxAxis,
            grid: { color: 'rgba(0,0,0,.08)' },
            angleLines: { color: 'rgba(0,0,0,.08)' },
            pointLabels: { color: '#333', font: { size: 12, weight: '600' } },
            ticks: { display: false }
          }
        }
      }
    });
  }

  // 3) PDF 다운로드
  async function downloadPDF() {
    const { jsPDF } = window.jspdf || {};
    if (!jsPDF) return alert('PDF 생성 모듈을 불러올 수 없습니다.');

    const pdf = new jsPDF({ unit: 'pt', format: 'a4' });
    const margin = 40;

    pdf.setFont('helvetica', 'bold');
    pdf.setFontSize(18);
    pdf.text('D-tect 분석 결과', margin, 50);

    const canvas = $('#radar');
    const img = canvas.toDataURL('image/png', 1.0);

    // 🔸 CSS 기반 크기일 때도 정확한 비율로 PDF에 배치
    const rect = canvas.getBoundingClientRect();
    const pageW = pdf.internal.pageSize.getWidth();
    const imgW = pageW - margin * 2;
    const imgH = (rect.height / rect.width) * imgW;

    pdf.addImage(img, 'PNG', margin, 80, imgW, imgH);

    // 수치 표
    const data = radarChart?.data;
    if (data) {
      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(12);
      let y = 100 + imgH;
      y += 16;
      pdf.text('세부 수치(%)', margin, y);
      y += 10;

      const labels = data.labels;
      const vals = data.datasets[0].data;
      labels.forEach((label, i) => {
        y += 18;
        pdf.text(`• ${label} : ${vals[i]}%`, margin, y);
      });
    }

    pdf.save('analysis_result.pdf');
  }

  // bootstrap
  (async () => {
    const dataset = await loadAnalysisData();
    renderRadar(dataset);
    $('#btnPdf')?.addEventListener('click', downloadPDF);
  })();
})();
