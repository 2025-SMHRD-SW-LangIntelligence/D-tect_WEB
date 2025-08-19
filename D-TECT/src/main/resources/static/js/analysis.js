// ---------- 설정 ----------
const CATEGORIES = [
    '성희롱·성적발언',
    '외모·신체 비하',
    '콘텐츠·실력비하',
    '혐오발언',
    '인신공격·모욕',
    '스팸·채팅도배'
];

// 분석 결과 샘플 생성기(백엔드 연결 전 데모)
// 각 항목 0~100 사이 값
function mockAnalyze() {
    return CATEGORIES.map(() => Math.floor(Math.random() * 40) + Math.floor(Math.random() * 40)); // 0~80 근사
}

// ---------- 엘리먼트 ----------
const titleEl = document.getElementById('pageTitle');
const panelLoading = document.getElementById('panelLoading');
const panelResult = document.getElementById('panelResult');
const btnAnalyze = document.getElementById('btnAnalyze');
const btnPdf = document.getElementById('btnPdf');
const btnAI = document.getElementById('btnAI');
const progressEl = document.getElementById('progress');
const canvas = document.getElementById('radar');
const ctx = canvas.getContext('2d');

// ---------- 이벤트 ----------
btnAI?.addEventListener('click', () => alert('챗봇을 준비중입니다. (여기에 챗봇 패널/모달을 연결하세요)'));

btnAnalyze.addEventListener('click', startAnalysis);
btnPdf.addEventListener('click', downloadPNG); // PDF 서버 연동 전: 차트 PNG 저장

// ---------- 분석 시뮬레이션 + 전환 ----------
function startAnalysis() {
    btnAnalyze.disabled = true;
    titleEl.textContent = '분석 진행중';

    let p = 0;
    progressEl.textContent = '0%';

    const tick = setInterval(() => {
        p += Math.floor(Math.random() * 7) + 3;  // 3~9% 증가
        if (p >= 100) { p = 100; clearInterval(tick); onAnalyzeDone(); }
        progressEl.textContent = `${p}%`;
    }, 180);
}

function onAnalyzeDone() {
    // 실제로는 서버에서 결과 받아오기
    const values = mockAnalyze(); // [..6개..]
    drawRadar(values);

    // 화면 전환
    panelLoading.classList.add('hidden');
    panelResult.classList.remove('hidden');
    titleEl.textContent = '분석 결과';
}

// ---------- 레이더 차트 (Canvas) ----------
function drawRadar(values) {
    // values: 6개 (0~100)
    const w = canvas.width, h = canvas.height;
    ctx.clearRect(0, 0, w, h);

    const cx = w / 2, cy = h / 2 + 10;
    const radius = Math.min(w, h) * 0.36;
    const steps = 5; // 기준 원 5개
    const axes = CATEGORIES.length;

    // 배경
    ctx.fillStyle = '#2a2a2a';
    ctx.fillRect(0, 0, w, h);

    // 기준 원/눈금
    ctx.strokeStyle = '#777';
    ctx.lineWidth = 1;
    for (let s = 1; s <= steps; s++) {
        const r = radius * (s / steps);
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.stroke();
    }

    // 축 + 라벨
    ctx.fillStyle = '#eaeaea';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    for (let i = 0; i < axes; i++) {
        const ang = -Math.PI / 2 + i * (Math.PI * 2 / axes);
        const x = cx + Math.cos(ang) * radius;
        const y = cy + Math.sin(ang) * radius;

        // 축선
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(x, y);
        ctx.strokeStyle = '#666';
        ctx.stroke();

        // 라벨
        const lx = cx + Math.cos(ang) * (radius + 28);
        const ly = cy + Math.sin(ang) * (radius + 28);
        wrapText(CATEGORIES[i], lx, ly, 120, 14);
    }

    // 값 폴리곤
    ctx.beginPath();
    for (let i = 0; i < axes; i++) {
        const v = Math.max(0, Math.min(100, values[i]));
        const r = radius * (v / 100);
        const ang = -Math.PI / 2 + i * (Math.PI * 2 / axes);
        const x = cx + Math.cos(ang) * r;
        const y = cy + Math.sin(ang) * r;
        if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.fillStyle = 'rgba(239,68,68,0.25)';   // 빨강 반투명
    ctx.strokeStyle = 'rgba(239,68,68,0.9)';
    ctx.lineWidth = 2;
    ctx.fill(); ctx.stroke();

    // 값 점
    for (let i = 0; i < axes; i++) {
        const v = Math.max(0, Math.min(100, values[i]));
        const r = radius * (v / 100);
        const ang = -Math.PI / 2 + i * (Math.PI * 2 / axes);
        const x = cx + Math.cos(ang) * r;
        const y = cy + Math.sin(ang) * r;
        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(239,68,68,0.95)';
        ctx.fill();
    }
}

// 텍스트 줄바꿈 유틸
function wrapText(text, x, y, maxWidth, lineHeight) {
    const words = text.split('');
    let line = '', lines = [];
    for (let i = 0; i < words.length; i++) {
        const test = line + words[i];
        const w = ctx.measureText(test).width;
        if (w > maxWidth && line !== '') {
            lines.push(line);
            line = words[i];
        } else {
            line = test;
        }
    }
    lines.push(line);
    const totalH = lines.length * lineHeight;
    let sy = y - totalH / 2 + lineHeight / 2;
    for (const ln of lines) {
        ctx.fillText(ln, x, sy);
        sy += lineHeight;
    }
}

// ---------- 내보내기(PNG) ----------
function downloadPNG() {
    // 차트 카드 전체 스냅샷 (캔버스만 저장)
    const a = document.createElement('a');
    a.href = canvas.toDataURL('image/png');
    a.download = 'd-tect-analysis.png';
    a.click();
}
