/* 결과 페이지에서 Chart.js, jspdf 등은 기존대로 로딩되어 있어야 합니다. */
/* D-tect 분석 결과 페이지 (analId 기준 & 최댓값=100 정규화) */
(() => {
	const $ = (sel, root = document) => root.querySelector(sel);

	// ---- analId 해석: URL → SSR → sessionStorage → (유효성 검사) ----
	function pickAnalIdRaw() {
		try {
			const urlId = new URLSearchParams(location.search).get('analId');
			if (urlId) return urlId;
		} catch { }
		if (typeof window !== 'undefined' && window.__analId != null) return String(window.__analId);
		try {
			const saved = sessionStorage.getItem('analysisAnalId');
			if (saved) return saved;
		} catch { }
		return null;
	}
	function sanitizeAnalId(raw) {
		if (raw == null) return null;
		const s = String(raw).trim();
		// "undefined" / "null" / 빈문자열 방지
		if (s === '' || s.toLowerCase() === 'undefined' || s.toLowerCase() === 'null') return null;
		if (!/^\d+$/.test(s)) return null; // 숫자만 허용
		return s;
	}
	function getAnalId() {
		return sanitizeAnalId(pickAnalIdRaw());
	}

	// ---- /api/analysis/{analId}/summary → {labels, values, title?} ----
	async function loadAnalysisData(analId) {
		if (analId) {
			try {
				const url = `/api/analysis/${encodeURIComponent(String(analId))}/summary`;
				console.debug('[analysis] fetch summary:', url);
				const res = await fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'include' });
				if (!res.ok) {
					const text = await res.text().catch(() => '');
					console.warn('[analysis] summary fetch failed', res.status, text);
				} else {
					const json = await res.json();
					if (json && Array.isArray(json.labels) && Array.isArray(json.values)) {
						try { localStorage.setItem('analysis:last', JSON.stringify(json)); } catch { }
						return json;
					} else {
						console.warn('[analysis] summary shape invalid:', json);
					}
				}
			} catch (e) {
				console.warn('[analysis] summary error:', e);
			}
		} else {
			console.warn('[analysis] analId is missing or invalid; using cache/sample');
		}
		// 캐시 → 샘플 폴백
		try {
			const cached = localStorage.getItem('analysis:last');
			if (cached) {
				const parsed = JSON.parse(cached);
				if (parsed && Array.isArray(parsed.labels) && Array.isArray(parsed.values)) return parsed;
			}
		} catch { }
		return {
			title: '샘플 결과',
			labels: ['폭력', '명예훼손', '성희롱', '따돌림', '협박', '공갈'],
			values: [22, 18, 12, 8, 26, 14]
		};
	}

	(async () => {
		const analId = getAnalId();
		console.debug('[analysis] resolved analId =', analId);

		// analId를 URL에 고정(유효할 때만)
		if (analId) {
			try {
				const u = new URL(location.href);
				if (!u.searchParams.get('analId')) {
					u.searchParams.set('analId', String(analId));
					history.replaceState(null, '', u.toString());
				}
			} catch { }
			try { sessionStorage.setItem('analysisAnalId', String(analId)); } catch { }
		}

		// ---- 최댓값=100 정규화 ----
		function normalizeTo100(values) {
			const nums = values.map(v => Number(v) || 0);
			const max = Math.max(0, ...nums);
			if (max <= 0) return nums.map(() => 0);
			return nums.map(v => Math.round((v / max) * 100));
		}

		// ---- 차트 ----
		let radarChart = null;
		function renderRadar({ labels, values, normalized }) {
			const canvas = $('#radar');
			if (!canvas || typeof Chart === 'undefined') return;
			const ctx = canvas.getContext('2d');

			if (radarChart) { radarChart.destroy(); radarChart = null; }

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
								label: (ctx) => {
									const i = ctx.dataIndex;
									const norm = normalized[i] ?? 0;
									const raw = values[i] ?? 0;
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

		// ---- PDF 저장 ----
		async function downloadPDF(analId, labels, values, normalized) {
			const { jsPDF } = window.jspdf || {};
			if (!jsPDF) return alert('PDF 생성 모듈을 불러올 수 없습니다.');

			const pdf = new jsPDF({ unit: 'pt', format: 'a4' });
			const margin = 40;

			pdf.setFont('helvetica', 'bold');
			pdf.setFontSize(18);
			pdf.text('D-tect 분석 결과', margin, 50);

			const canvas = $('#radar');
			if (!canvas) return;

			const img = canvas.toDataURL('image/png', 1.0);
			const pageW = pdf.internal.pageSize.getWidth();
			const imgW = pageW - margin * 2;
			const imgH = (canvas.height / canvas.width) * imgW;

			pdf.addImage(img, 'PNG', margin, 80, imgW, imgH);

			pdf.setFont('helvetica', 'normal');
			pdf.setFontSize(12);
			let y = 100 + imgH + 16;
			pdf.text('세부 수치 (정규화% / 원시 건수)', margin, y);
			y += 10;

			labels.forEach((label, i) => {
				y += 18;
				const norm = normalized[i] ?? 0;
				const raw = values[i] ?? 0;
				pdf.text(`• ${label} : ${norm}%  (원시 ${raw}건)`, margin, y);
			});

			const name = analId ? `analysis_result_${analId}.pdf` : 'analysis_result.pdf';
			pdf.save(name);
		}

		// ---- Bootstrap ----
		(async () => {
			const analId = getAnalId();

			// analId를 URL에 고정(유효할 때만)
			if (analId) {
				try {
					const u = new URL(location.href);
					if (!u.searchParams.get('analId')) {
						u.searchParams.set('analId', analId);
						history.replaceState(null, '', u.toString());
					}
				} catch { }
				try { sessionStorage.setItem('analysisAnalId', analId); } catch { }
			}

			const data = await loadAnalysisData(analId);
			const labels = data.labels ?? [];
			const values = data.values ?? [];
			const normalized = normalizeTo100(values);

			const titleEl = $('#resultTitle');
			if (titleEl) titleEl.textContent = data.title || '분석 결과';

			const analIdEl = $('#analIdText');
			if (analIdEl && analId) analIdEl.textContent = `#${analId}`;

			renderRadar({ labels, values, normalized });

			const pdfBtn = $('#btnPdf');
			if (pdfBtn) {
				pdfBtn.addEventListener('click', () => downloadPDF(analId, labels, values, normalized));
			}
		})();
	})();
});