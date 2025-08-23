// ===== 기본 경로/ID =====
const SCHEDULE_PATH = "/expert_schedule.html";
const EXPERT_ID = (() => {
    const byBody = Number(document.body.dataset.expertId || 0);
    if (byBody) return byBody;
    const qs = new URLSearchParams(location.search);
    return Number(qs.get("expertId") || 0);
})();
if (!EXPERT_ID) console.warn("expertId가 바인딩되지 않았습니다.");

// ===== 달력 =====
const grid = document.getElementById("calendarGrid");
const monthChip = document.getElementById("monthChip");
const calendarCard = document.getElementById("calendarCard");

let current = new Date(); current.setDate(1);
function makeGrid() {
    if (!grid) return;
    grid.innerHTML = "";
    const y = current.getFullYear();
    const m = current.getMonth();
    if (monthChip) monthChip.textContent = `${m + 1}월`;

    const firstDay = new Date(y, m, 1);
    const lastDay  = new Date(y, m + 1, 0);
    const startIdx = (firstDay.getDay() + 7) % 7;
    const total    = lastDay.getDate();
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
calendarCard?.addEventListener("click", () => location.href = SCHEDULE_PATH);
calendarCard?.addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === " ") { e.preventDefault(); location.href = SCHEDULE_PATH; }
});

// ===== 요소 =====
const reqList   = document.getElementById("requestList");
const reqEmpty  = document.getElementById("requestEmpty");
const alertList = document.getElementById("alertList");
const statToday   = document.getElementById("statToday");
const statPending = document.getElementById("statPending");
const statUnread  = document.getElementById("statUnread");
const statTasks   = document.getElementById("statTasks");

// 상세 모달
const detailModal = document.getElementById("detailModal");
const detailClose = document.getElementById("detailClose");
const btnApprove  = document.getElementById("detailApprove");
const btnReject   = document.getElementById("detailReject");
const dDate   = document.getElementById("dDate");
const dUser   = document.getElementById("dUser");
const dReason = document.getElementById("dReason");
const dMsg    = document.getElementById("dMsg");
const dFile   = document.getElementById("dFile");

// 결과 알림 모달
const noticeModal = document.getElementById("noticeModal");
const noticeClose = document.getElementById("noticeClose");
const noticeOk    = document.getElementById("noticeOk");
const noticeMsg   = document.getElementById("noticeMsg");

// ===== 모달 유틸 =====
function openModal(modalEl) {
    if (!modalEl) return;
    modalEl._returnFocus = document.activeElement;
    modalEl.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
    setTimeout(() => {
        modalEl.querySelector("[autofocus], .modal__actions .btn, .icon-btn, button,[href],input,select,textarea,[tabindex]:not([tabindex='-1'])")?.focus();
    }, 0);
}
function closeModal(modalEl) {
    if (!modalEl) return;
    if (modalEl.contains(document.activeElement)) document.activeElement.blur();
    modalEl.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    modalEl._returnFocus?.focus?.();
}
document.addEventListener("click", (e) => {
    if (e.target?.dataset?.close) closeModal(e.target.closest(".modal"));
});
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        if (noticeModal.getAttribute("aria-hidden") === "false") closeModal(noticeModal);
        else if (detailModal.getAttribute("aria-hidden") === "false") closeModal(detailModal);
    }
});
detailClose?.addEventListener("click", () => closeModal(detailModal));
noticeClose?.addEventListener("click", () => closeModal(noticeModal));
noticeOk?.addEventListener("click", () => closeModal(noticeModal));

function showNotice(message) {
    noticeMsg.textContent = message || "처리되었습니다.";
    openModal(noticeModal);
}

// ===== 유틸 =====
const fmtDate = (isoOrTs) => {
    if (!isoOrTs) return "-";
    try {
        const d = new Date(isoOrTs);
        if (isNaN(d.getTime())) return String(isoOrTs);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        return `${y}-${m}-${day}`;
    } catch { return String(isoOrTs); }
};
const pick = (obj, ...keys) => {
    const k = keys.find(k => obj && obj[k] != null);
    return k ? obj[k] : undefined;
};
const REASON_LABELS = {
    VIOLENCE:"폭력", DEFAMATION:"명예훼손", STALKING:"스토킹", SEXUAL:"성범죄",
    LEAK:"정보유출", BULLYING:"따돌림/집단괴롭힘", CHANTAGE:"협박/갈취", EXTORTION:"공갈/갈취",
};
const toReasonLabel = (v) => {
    if (!v) return "-";
    const up = String(v).toUpperCase();
    return REASON_LABELS[up] || v;
};

// ===== API =====
async function fetchMatchings() {
    const url = `/mypage/api/expert/${EXPERT_ID}/matchings`;
    const res = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!res.ok) throw new Error("목록 로드 실패: " + res.status);
    return res.json();
}
async function fetchDetail(id) {
    const res = await fetch(`/api/matching/${id}`, { headers: { "Accept": "application/json" } });
    if (!res.ok) throw new Error("상세 로드 실패: " + res.status);
    return res.json();
}
async function postAction(matchingId, action) {
    const res = await fetch(`/api/matching/${matchingId}/${action}`, { method: "POST" });
    let data = null; try { data = await res.json(); } catch {}
    if (!res.ok) throw new Error(data?.error || `${action} 실패: ${res.status}`);
    return data;
}

// ===== 리스트 렌더 =====
function rowHTML(row) {
    const id    = pick(row, "matchingId", "id", "matchingIdx");
    const reqAt = pick(row, "requestedAt", "requestTime", "requestedDate");
    const user  = pick(row, "userNameMasked", "userName", "username", "name", "customerName") || "-";
    const reasonRaw = pick(row, "reasonLabel", "requestReasonLabel", "typeLabel", "reason", "requestReason");
    const reason = toReasonLabel(reasonRaw);

    return `
    <li class="req-item">
      <div class="req-date">${fmtDate(reqAt)}</div>
      <div class="req-name">${user} · ${reason}</div>
      <div class="req-state">
        <button class="btn btn--mini js-detail" data-id="${id}">자세히보기</button>
      </div>
    </li>`;
}

// 메인페이지는 '대기(PENDING)'만 보여주도록 필터링
function renderList(rows) {
    const pendingOnly = (rows || []).filter(r =>
        String(pick(r, "status")).toUpperCase() === "PENDING"
    );

    if (pendingOnly.length === 0) {
        reqList.innerHTML = "";
        reqEmpty.hidden = false;
        return;
    }
    reqEmpty.hidden = true;

    const sorted = [...pendingOnly].sort((a, b) => {
        const ax = pick(a, "requestedAt", "matchingId", "id");
        const bx = pick(b, "requestedAt", "matchingId", "id");
        return String(bx).localeCompare(String(ax));
    });
    reqList.innerHTML = sorted.map(rowHTML).join("");
}

// ===== 알림/통계 =====
function renderAlertsAndStats(rows) {
    const todayISO = new Date().toISOString().slice(0, 10);
    const todayCnt = rows.filter(r => {
        const d = pick(r, "approvedAt", "matchedAt", "requestedAt");
        return d && String(fmtDate(d)) === todayISO;
    }).length;

    const pendingCnt  = rows.filter(r => String(pick(r, "status")).toUpperCase() === "PENDING").length;
    const approvedCnt = rows.filter(r => String(pick(r, "status")).toUpperCase() === "APPROVED").length;
    const unreadChats = 0; // TODO
    const openTasks   = pendingCnt + approvedCnt;

    statToday.textContent   = String(todayCnt);
    statPending.textContent = String(pendingCnt);
    statUnread.textContent  = String(unreadChats);
    statTasks.textContent   = String(openTasks);

    const alerts = [
        { type: "공지",  msg: "8월 시스템 점검 안내 (08/25 02:00~04:00)" },
        { type: "할일",  msg: `승인대기 ${pendingCnt}건` },
        { type: "메시지", msg: `읽지 않은 채팅 ${unreadChats}개` },
    ];
    alertList.innerHTML = alerts.map(a => `
    <li class="alert-item">
      <div class="alert-type">${a.type}</div>
      <div>${a.msg}</div>
      <div></div>
    </li>`).join("");
}

// ===== 행 클릭: 상세 =====
reqList.addEventListener("click", async (e) => {
    const btn = e.target.closest(".js-detail");
    if (!btn) return;
    const id = btn.dataset.id;
    try {
        const d = await fetchDetail(id);
        detailModal.dataset.id = id;
        dDate.textContent   = d.requestedAt ? fmtDate(d.requestedAt) : "-";
        dUser.textContent   = d.userName || "-";
        dReason.textContent = d.reasonLabel || toReasonLabel(d.reasonCode) || "-";
        dMsg.textContent    = d.message || "-";
        dFile.href          = d.attachmentUrl || "#";
        openModal(detailModal);
    } catch {
        showNotice("상세 정보를 불러오지 못했습니다.");
    }
});

// ===== 상세 모달에서 승인/거절 =====
btnApprove?.addEventListener("click", async () => {
    const id = detailModal.dataset.id;
    if (!id) return;
    btnApprove.disabled = btnReject.disabled = true;
    try {
        await postAction(id, "approve");
        closeModal(detailModal);
        showNotice("승인 처리되었습니다.");
        await reloadAll();
    } catch (err) {
        showNotice(err.message || "승인에 실패했습니다.");
    } finally {
        btnApprove.disabled = btnReject.disabled = false;
    }
});
btnReject?.addEventListener("click", async () => {
    const id = detailModal.dataset.id;
    if (!id) return;
    btnApprove.disabled = btnReject.disabled = true;
    try {
        await postAction(id, "reject");
        closeModal(detailModal);
        showNotice("거절 처리되었습니다.");
        await reloadAll();
    } catch (err) {
        showNotice(err.message || "거절에 실패했습니다.");
    } finally {
        btnApprove.disabled = btnReject.disabled = false;
    }
});

// ===== 로드 & 갱신 =====
async function reloadAll() {
    try {
        const rows = await fetchMatchings();
        renderList(rows);            // 화면 리스트: PENDING만
        renderAlertsAndStats(rows);  // 통계/알림: 전체 데이터 기준
    } catch (e) {
        reqList.innerHTML = "";
        reqEmpty.hidden = false;
        alertList.innerHTML = `
      <li class="alert-item">
        <div class="alert-type">오류</div>
        <div>목록을 불러오지 못했습니다.</div>
        <div></div>
      </li>`;
    }
}
reloadAll();
