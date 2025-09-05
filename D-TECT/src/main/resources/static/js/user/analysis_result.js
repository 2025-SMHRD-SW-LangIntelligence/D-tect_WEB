 (() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  // ---- analId 해석 (dev 방식을 유지) ----
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

  // ---- summary API 호출 (dev) ----
  async function loadAnalysisData(analId) {
  if (!analId) return null;
  try {
  const url = `/api/analysis/${encodeURIComponent(String(analId))}/summary`;
  const res = await fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'include' });
  if (res.ok) {
  const json = await res.json();
  if (json && Array.isArray(json.labels) && Array.isArray(json.values)) return json;
}
} catch (e) {
  console.warn('[analysis] summary error:', e);
}
  return null;
}

  // ---- PDF 준비 UI 적용 (dev 버튼 + 팀원 타일 모두 지원) ----
  function applyPdfReadyUI(reportUrl, name) {
  // dev: 버튼이 있는 경우
  const btn = $('#btnPdf');
  if (btn) {
  btn.classList.remove('hidden');
  btn.onclick = (e) => {
  e.preventDefault();
  const a = document.createElement('a');
  a.href = reportUrl;
  // 서버가 Content-Disposition을 주면 그걸 사용.
  // 이름을 강제하려면 아래 한 줄을 풀어도 됨(필요 시).
  // a.download = name ? `${name}의 결과 보고서.pdf` : '';
  a.download = '';
  a.target = '_blank';
  document.body.appendChild(a);
  a.click();
  a.remove();
};
}

  // 팀원: 타일이 있는 경우
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
  // 팀원 기대치: name 있으면 파일명 지정, 없으면 서버 헤더/브라우저 기본 사용
  if (name) a.download = `${name}의 결과 보고서.pdf`;
  else a.download = '';
  document.body.appendChild(a);
  a.click();
  a.remove();
}, { once: true });
}
}

  // ---- SSE로 PDF 준비 알림 (dev) ----
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
  es.onerror = () => {
  // 네트워크/서버 문제 시 브라우저가 재시도하긴 함.
  // 폴백 폴링이 별도로 돌고 있으니 여기선 조용히 둔다.
};
  return () => { closed = true; try { es.close(); } catch {} };
} catch (e) {
  console.warn('[analysis] SSE 연결 실패:', e);
  return () => {};
}
}

  // ---- 폴백: report 폴링 (팀원) ----
  async function fetchReportInfo(analId) {
  try {
  const res = await fetch(`/api/analysis/${analId}/report`, {
  headers: { 'Accept': 'application/json' },
  credentials: 'include'
});
  if (res.ok) return await res.json(); // { reportUrl, name? ... }
} catch (e) {
  // console.warn('[analysis] report fetch error', e);
}
  return null;
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
  // 타일 UI가 있다면 "아직 준비되지 않음" 메시지를 유지(팀원 UX 준수).
  const tile = $('#pdfTile'), caption = $('#pdfCaption'), spinner = $('#pdfSpinner');
  if (tile && caption) {
  tile.hidden = false;
  tile.style.pointerEvents = 'none';
  caption.textContent = '아직 준비되지 않음';
  if (spinner) spinner.style.display = 'none';
}
}

  // ---- 차트 렌더링 (dev 스타일 유지) ----
  function normalizeTo100(values) {
  const nums = values.map(v => Number(v) || 0);
  const max = Math.max(0, ...nums);
  if (max <= 0) return nums.map(() => 0);
  return nums.map(v => Math.round((v / max) * 100));
}

  let radarChart = null;
  function renderRadar({ labels, values }) {
  const canvas = $('#radar');
  if (!canvas || typeof Chart === 'undefined') return;
  const ctx = canvas.getContext('2d');
  if (radarChart) radarChart.destroy();

  const normalized = normalizeTo100(values);

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
  plugins: { legend: { display: false } },
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

  // 차트
  const data = await loadAnalysisData(analId);
  if (data && data.labels?.length) renderRadar({ labels: data.labels, values: data.values });

  // PDF 준비 감지: SSE 우선 + 폴링 백업
  let pdfReady = false;
  const stopIf = () => pdfReady === true;
  const onReady = (url, name) => { if (!pdfReady) { pdfReady = true; applyPdfReadyUI(url, name); } };

  const stopSse = listenPdfReady(analId, onReady);
  // 타일 UI가 있다면 로딩 상태 보이기
  const tile = $('#pdfTile'), caption = $('#pdfCaption'), spinner = $('#pdfSpinner');
  if (tile && caption) {
  tile.hidden = false;
  tile.style.pointerEvents = 'none';
  caption.textContent = 'PDF 준비 중...';
  if (spinner) spinner.style.display = 'inline-block';
}
  // 폴링 시작 (SSE가 선착하면 stopIf가 true가 되어 중단)
  pollReportReady(analId, { tries: 60, intervalMs: 1000 }, onReady, stopIf);

  // 페이지 떠날 때 SSE 닫기
  window.addEventListener('pagehide', () => { try { stopSse(); } catch {} }, { once: true });
})();
})();
