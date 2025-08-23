// ✅ 경로 설정
const SCHEDULE_PATH = "/expert_schedule.html";   // 달력 상세
const MYPAGE_PATH = "/expertMyinfoPage";     // 마이페이지

// 요소
const grid = document.getElementById("calendarGrid");
const monthChip = document.getElementById("monthChip");
const calendarCard = document.getElementById("calendarCard");
const mypageBtn = document.getElementById("mypageBtn");

// ── 달력 렌더 ─────────────────────────────────────────
let current = new Date(); current.setDate(1);
function pad(n) { return String(n).padStart(2, "0"); }

function makeGrid() {
    grid.innerHTML = "";
    const y = current.getFullYear();
    const m = current.getMonth();
    monthChip.textContent = `${m + 1}월`;

    const firstDay = new Date(y, m, 1);
    const lastDay = new Date(y, m + 1, 0);
    const startIdx = (firstDay.getDay() + 7) % 7;
    const total = lastDay.getDate();
    const prevLast = new Date(y, m, 0).getDate();

    for (let i = 0; i < 42; i++) {
        const cell = document.createElement("div");
        cell.className = "cell";
        if (i < startIdx) {
            cell.classList.add("cell--other");
            cell.innerHTML = `<span class="num">${prevLast - (startIdx - 1 - i)}</span>`;
        } else if (i >= startIdx + total) {
            cell.classList.add("cell--other");
            cell.innerHTML = `<span class="num">${i - (startIdx + total) + 1}</span>`;
        } else {
            cell.innerHTML = `<span class="num">${i - startIdx + 1}</span>`;
        }
        grid.appendChild(cell);
    }
}
makeGrid();

// 클릭 시 상세 페이지로 이동
function goDetail() { window.location.href = SCHEDULE_PATH; }
calendarCard.addEventListener("click", goDetail);
calendarCard.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); goDetail(); } });

// 마이페이지 아이콘 이동(링크 fallback)
mypageBtn.addEventListener("click", (e) => {
    // a href 로 이동하지만, 필요시 JS 네비게이션 유지
    if (!mypageBtn.getAttribute("href")) window.location.href = MYPAGE_PATH;
});

// ── 상담 요청 더미 데이터 & 렌더 ──────────────────────
const REQUESTS = [
    { date: "2025-08-14", name: "강짱돌", type: "명예훼손", status: "매칭완료" },
    { date: "2025-08-16", name: "박○○", type: "상담예약", status: "승인대기" },
    { date: "2025-08-18", name: "이○○", type: "정책지원", status: "반려" },
];
const reqList = document.getElementById("requestList");
function badge(stat) {
    if (/반려/.test(stat)) return `<span class="badge badge--rejected">${stat}</span>`;
    if (/대기/.test(stat)) return `<span class="badge badge--pending">${stat}</span>`;
    return `<span class="badge badge--approved">${stat}</span>`;
}
function renderReq() {
    reqList.innerHTML = REQUESTS.map(r => `
    <li class="req-item">
      <div class="req-date">${r.date}</div>
      <div class="req-name">${r.name} · ${r.type}</div>
      <div class="req-state">${badge(r.status)}</div>
    </li>
  `).join("");
}
renderReq();

// ── 알림 센터(공지/할일/시스템 알림) ───────────────────
const ALERTS = [
    { type: "공지", msg: "8월 시스템 점검 안내 (08/25 02:00~04:00)" },
    { type: "할일", msg: "승인대기 1건 확인 필요" },
    { type: "메시지", msg: "읽지 않은 채팅 3개" },
];
const alertList = document.getElementById("alertList");
function renderAlerts() {
    alertList.innerHTML = ALERTS.map(a => `
    <li class="alert-item">
      <div class="alert-type">${a.type}</div>
      <div>${a.msg}</div>
      <div></div>
    </li>
  `).join("");
}
renderAlerts();

// ── 요약 통계(오늘 일정/승인대기/읽지 않은 채팅/미완료 작업) ─────
const todayISO = new Date().toISOString().slice(0, 10);
const todayCount = REQUESTS.filter(r => r.date === todayISO).length;
const pendingCnt = REQUESTS.filter(r => /대기/.test(r.status)).length;
const unreadChats = 3;  // 예시 값 (API 연동 시 교체)
const openTasks = 1;  // 예시 값

document.getElementById("statToday").textContent = todayCount;
document.getElementById("statPending").textContent = pendingCnt;
document.getElementById("statUnread").textContent = unreadChats;
document.getElementById("statTasks").textContent = openTasks;

