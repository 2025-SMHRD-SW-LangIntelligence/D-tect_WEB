const REFRESH_MS = 5000;

function resolveMatchingId() {
    // path: /chat/room/123
    const parts = location.pathname.split("/").filter(Boolean);
    const idx = parts.findIndex(p => p === "room");
    if (idx >= 0 && parts[idx + 1]) return Number(parts[idx + 1]);

    // query: ?matching=123
    const q = new URLSearchParams(location.search);
    return Number(q.get("matching") || 0);
}
function resolveMyRole() {
    const q = new URLSearchParams(location.search);
    const v = (q.get("me") || "").toLowerCase();
    return v === "expert" ? "expert" : "user"; // default: user
}

const MATCHING_ID = resolveMatchingId();
const MY_ROLE = resolveMyRole(); // 'user' | 'expert'

if (!MATCHING_ID) {
    alert("유효하지 않은 채팅방 주소입니다. (matchingId 없음)");
}

/* ========= DOM ========= */
const listEl     = document.getElementById("messageList");
const composerEl = document.getElementById("composer");
const sendBtn    = document.getElementById("sendBtn");
const lastUpdEl  = document.getElementById("lastUpdated");
const refreshBtn = document.getElementById("refreshBtn");
const jumpNewBtn = document.getElementById("jumpNew");

const fileListEl = document.getElementById("fileList");
const fileInput  = document.getElementById("fileInput");
const uploadBtn  = document.getElementById("uploadBtn");

/* ========= 상태 ========= */
let seenIds      = new Set();
let pollingTimer = null;
let pendingPoll  = false;
let composing    = false;
let unseenCount  = 0;

/* ========= 유틸 ========= */
const fmt = (ts) => {
    const d = new Date(ts);
    if (Number.isNaN(d.getTime())) return "-";
    return d.toLocaleString("ko-KR", { hour12: false });
};
const nearBottom = () => (listEl.scrollHeight - listEl.scrollTop - listEl.clientHeight) < 80;
const scrollToBottom = () => (listEl.scrollTop = listEl.scrollHeight);
function escapeHtml(s) {
    return (s || "").replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));
}

/* ========= 서버 API 래퍼 =========
   서버 구현 상황에 따라 JSON/폼 방식을 모두 시도 (호환 위해)
*/
async function apiListMessages() {
    // GET /api/chat/{matchingId}/messages
    const res = await fetch(`/api/chat/${encodeURIComponent(MATCHING_ID)}/messages`, {
        headers: { "Accept": "application/json" }
    });
    if (!res.ok) throw new Error("메시지 조회 실패");
    // 기대 스키마: [{id, chatContent, senderType, chatedAt, sendedFile}]
    const arr = await res.json();
    // 프론트 표준으로 매핑
    return (arr || []).map(m => ({
        id: m.id ?? m.chatIdx ?? m.messageId,
        role: (String(m.senderType || "").toUpperCase() === "EXPERT") ? "expert" : "user",
        name: (String(m.senderType || "").toUpperCase() === "EXPERT") ? "전문가" : "사용자",
        text: m.chatContent ?? m.content ?? m.text ?? "",
        ts:   m.chatedAt ?? m.createdAt ?? m.ts ?? Date.now(),
        file: m.sendedFile || null
    }));
}

async function apiPostMessage(text) {
    // 우선 JSON으로 시도
    let ok = false, bodyJson = { content: text, role: MY_ROLE };
    try {
        const r = await fetch(`/api/chat/${encodeURIComponent(MATCHING_ID)}/messages`, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Accept": "application/json" },
            body: JSON.stringify(bodyJson)
        });
        ok = r.ok;
        if (!ok && r.status === 415) throw new Error("fallback"); // 형식 불일치 대비
        if (!ok) throw new Error("전송 실패");
        return await r.json().catch(() => ({}));
    } catch {
        // 폼 전송 폴백(@RequestParam content, meRole 등)
        const fd = new FormData();
        fd.append("content", text);
        fd.append("meRole", MY_ROLE); // 서버가 role 파라미터 이름을 meRole로 받을 수도 있어 폴백

        const r2 = await fetch(`/api/chat/${encodeURIComponent(MATCHING_ID)}/messages`, { method: "POST", body: fd });
        if (!r2.ok) throw new Error("전송 실패");
        return await r2.json().catch(() => ({}));
    }
}

async function apiListFiles() {
    // GET /api/chat/{matchingId}/files  (없으면 조용히 무시)
    try {
        const res = await fetch(`/api/chat/${encodeURIComponent(MATCHING_ID)}/files`, {
            headers: { "Accept": "application/json" }
        });
        if (!res.ok) return [];
        const arr = await res.json();
        // 기대 스키마: [{id,fileName,url,createdAt,by:'USER'|'EXPERT'}]
        return (arr || []).map(f => ({
            id:  f.id ?? f.fileIdx,
            url: f.url ?? (`/api/chat/${MATCHING_ID}/files/${f.id}`),
            ts:  f.createdAt ?? f.ts ?? Date.now(),
            name: f.fileName ?? "첨부",
            by:   { role: (String(f.by || f.senderType || "").toUpperCase() === "EXPERT") ? "expert" : "user" }
        }));
    } catch {
        return [];
    }
}

async function apiUploadFile(file) {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("role", MY_ROLE);

    const r = await fetch(`/api/chat/${encodeURIComponent(MATCHING_ID)}/files`, { method: "POST", body: fd });
    if (!r.ok) throw new Error("파일 업로드 실패");
    return await r.json().catch(() => ({}));
}

/* ========= 렌더 ========= */
function renderMessages(items) {
    if (!items?.length) return;
    const atBottom = nearBottom();

    const frag = document.createDocumentFragment();
    for (const m of items) {
        const idStr = String(m.id ?? "");
        if (idStr && seenIds.has(idStr)) continue; // 중복 방지
        if (idStr) seenIds.add(idStr);

        const row = document.createElement("div");
        row.className = "row " + (m.role === MY_ROLE ? "me" : "other");

        const msg = document.createElement("div");
        msg.className = "msg";
        msg.dataset.id = idStr || "";

        const delBtn = (m.role === MY_ROLE) ? `<button class="del-btn" data-id="${escapeHtml(idStr)}" aria-label="메시지 삭제">×</button>` : "";
        const fileLink = m.file ? `<div class="file"><a href="${escapeHtml(m.file)}" download>첨부 다운로드</a></div>` : "";

        msg.innerHTML = `
      <div class="meta">
        <span class="badge">${m.role === "expert" ? "변호사" : "사용자"}</span>
        <span class="name">${escapeHtml(m.name || (m.role === "expert" ? "전문가" : "사용자"))}</span>
        <span class="time">${fmt(m.ts || Date.now())}</span>
      </div>
      <div class="body">${escapeHtml(m.text || "")}</div>
      ${fileLink}
      ${delBtn}
    `;
        row.appendChild(msg);
        frag.appendChild(row);
    }

    listEl.appendChild(frag);

    if (atBottom) scrollToBottom();
    else {
        unseenCount += items.length;
        jumpNewBtn.textContent = `새 글 ${unseenCount}개`;
        jumpNewBtn.hidden = false;
    }
}

function renderFiles(items) {
    fileListEl.innerHTML = (items || []).map(f => `
    <li>
      <div class="meta">
        <span class="badge ${f.by?.role === 'expert' ? 'expert' : 'user'}">${f.by?.role === 'expert' ? '변호사' : '사용자'}</span>
        <span class="name">${escapeHtml(f.name)}</span>
        <span class="time">${fmt(f.ts)}</span>
      </div>
      <a class="dl" href="${f.url}" download>다운로드</a>
    </li>
  `).join("");
}

/* ========= 폴링 ========= */
async function poll() {
    if (pendingPoll) return;
    pendingPoll = true;
    listEl.setAttribute("aria-busy", "true");
    try {
        const msgs = await apiListMessages();
        renderMessages(msgs);

        const files = await apiListFiles();
        renderFiles(files);

        lastUpdEl.textContent = `마지막 업데이트 ${fmt(Date.now())}`;
    } catch (err) {
        console.error(err);
        lastUpdEl.textContent = "네트워크 오류로 갱신 실패";
    } finally {
        listEl.setAttribute("aria-busy", "false");
        pendingPoll = false;
    }
}
function startPolling() {
    if (pollingTimer) clearInterval(pollingTimer);
    pollingTimer = setInterval(poll, REFRESH_MS);
}

/* ========= 전송/삭제 ========= */
async function sendMessage() {
    const text = (composerEl.value || "").trim();
    if (!text) return;

    // 낙관적 UI
    renderMessages([{ id: `tmp_${Date.now()}`, role: MY_ROLE, name: "나", text, ts: Date.now() }]);
    composerEl.value = "";
    scrollToBottom();

    try {
        await apiPostMessage(text);
        await poll(); // 서버 상태 동기화
    } catch (err) {
        alert("전송 실패. 네트워크를 확인해주세요.");
        console.error(err);
    }
}

async function deleteMessage(messageId) {
    if (!confirm("이 메시지를 삭제하시겠습니까?")) return;
    try {
        const r = await fetch(`/api/chat/${encodeURIComponent(MATCHING_ID)}/messages/${encodeURIComponent(messageId)}`, {
            method: "DELETE"
        });
        if (!r.ok) throw new Error("삭제 실패");
        const el = listEl.querySelector(`.msg[data-id="${CSS.escape(String(messageId))}"]`);
        if (el) el.closest(".row")?.remove();
    } catch (err) {
        alert("삭제에 실패했습니다.");
        console.error(err);
    }
}

/* ========= 이벤트 ========= */
sendBtn.addEventListener("click", sendMessage);

// Enter 전송 (Shift+Enter 줄바꿈, 한글 조합 중 제외)
composerEl.addEventListener("compositionstart", () => composing = true);
composerEl.addEventListener("compositionend", () => composing = false);
composerEl.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey && !composing) {
        e.preventDefault();
        sendMessage();
    }
});

listEl.addEventListener("click", (e) => {
    const btn = e.target.closest(".del-btn");
    if (!btn) return;
    const msg = btn.closest(".msg");
    if (!msg) return;
    deleteMessage(msg.dataset.id);
});

refreshBtn.addEventListener("click", poll);
jumpNewBtn.addEventListener("click", () => {
    unseenCount = 0;
    jumpNewBtn.hidden = true;
    scrollToBottom();
});

uploadBtn.addEventListener("click", () => fileInput.click());
fileInput.addEventListener("change", async () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    try {
        await apiUploadFile(file);
        fileInput.value = "";
        poll();
    } catch (err) {
        alert("파일 업로드 실패");
        console.error(err);
    }
});

// 스크롤이 하단 근처면 새 글 배지 숨김
listEl.addEventListener("scroll", () => {
    if (nearBottom()) {
        unseenCount = 0;
        jumpNewBtn.hidden = true;
    }
});

/* ========= 초기화 ========= */
(async function init() {
    await poll();
    startPolling();
})();
