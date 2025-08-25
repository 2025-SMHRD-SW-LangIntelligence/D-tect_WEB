const REFRESH_MS = 5000;

function resolveMatchingId() {
    const parts = location.pathname.split("/").filter(Boolean);
    const idx = parts.findIndex(p => p === "room");
    if (idx >= 0 && parts[idx + 1]) return Number(parts[idx + 1]);
    const q = new URLSearchParams(location.search);
    return Number(q.get("matching") || 0);
}
function resolveMyRole() {
    const q = new URLSearchParams(location.search);
    const v = (q.get("me") || "").toLowerCase();
    return v === "expert" ? "expert" : "user";
}
function resolveMyMemIdx() {
    const q = new URLSearchParams(location.search);
    const v = Number(q.get("mem") || 0);
    return Number.isFinite(v) ? v : 0;
}

const MATCHING_ID = resolveMatchingId();
const MY_ROLE     = resolveMyRole();     // 'user' | 'expert'
const ME_MEM_IDX  = resolveMyMemIdx();   // ✔ 서버가 요구하는 meMemIdx

if (!MATCHING_ID) alert("유효하지 않은 채팅방 주소입니다. (matchingId 없음)");
if (!ME_MEM_IDX)  console.warn("경고: meMemIdx가 0입니다. (URL에 mem= 누락?)");

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

/* ========= 서버 API ========= */
async function apiListMessages() {
    const url = `/api/chat/${encodeURIComponent(MATCHING_ID)}/messages?meMemIdx=${encodeURIComponent(ME_MEM_IDX)}`;
    const res = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!res.ok) throw new Error("메시지 조회 실패");
    const arr = await res.json();
    return (arr || []).map(m => ({
        id:   m.id ?? m.chatIdx ?? m.messageId,
        role: (String(m.senderType || "").toUpperCase() === "EXPERT") ? "expert" : "user",
        name: (String(m.senderType || "").toUpperCase() === "EXPERT") ? "전문가" : "사용자",
        text: m.chatContent ?? m.content ?? m.text ?? "",
        ts:   m.chatedAt ?? m.createdAt ?? m.ts ?? Date.now(),
        file: m.sendedFile || null
    }));
}

async function apiPostMessage(text) {
    const fd = new FormData();
    fd.append("meMemIdx", String(ME_MEM_IDX)); // ✔ 컨트롤러 @RequestParam
    fd.append("content", text);
    // fd.append("file", fileTokenOrPath);

    const url = `/api/chat/${encodeURIComponent(MATCHING_ID)}/messages`;
    const r = await fetch(url, { method: "POST", body: fd });
    if (!r.ok) throw new Error("전송 실패");
    return await r.json().catch(() => ({}));
}

async function apiListFiles() {
    try {
        const url = `/api/chat/${encodeURIComponent(MATCHING_ID)}/files?meMemIdx=${encodeURIComponent(ME_MEM_IDX)}`;
        const res = await fetch(url, { headers: { "Accept": "application/json" } });
        if (!res.ok) return [];
        const arr = await res.json();
        return (arr || []).map(f => ({
            id:  f.id ?? f.fileIdx,
            url: f.url ?? (`/api/chat/${MATCHING_ID}/files/${f.id}?meMemIdx=${ME_MEM_IDX}`),
            ts:  f.createdAt ?? f.ts ?? Date.now(),
            name: f.fileName ?? "첨부",
            by:   { role: (String(f.by || f.senderType || "").toUpperCase() === "EXPERT") ? "expert" : "user" }
        }));
    } catch { return []; }
}

async function apiUploadFile(file) {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("meMemIdx", String(ME_MEM_IDX));
    const url = `/api/chat/${encodeURIComponent(MATCHING_ID)}/files`;
    const r = await fetch(url, { method: "POST", body: fd });
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

        if (idStr) {
            const bodyText = (m.text || "").trim();
            const tempMsg = Array.from(listEl.querySelectorAll('.msg[data-temp="1"]'))
                .find(el => {
                    const roleClass = el.closest('.row')?.classList.contains('me') ? 'me' : 'other';
                    const isMine = (m.role === MY_ROLE);
                    const elText = el.querySelector('.body')?.textContent?.trim() || '';
                    return (isMine && roleClass === 'me' && elText === bodyText);
                });
            if (tempMsg) {
                tempMsg.dataset.id = idStr;
                tempMsg.removeAttribute('data-temp');
                seenIds.add(idStr);
                continue; // 새로 append하지 않음
            }
        }

        if (idStr && seenIds.has(idStr)) continue;
        if (idStr) seenIds.add(idStr);

        const row = document.createElement("div");
        row.className = "row " + (m.role === MY_ROLE ? "me" : "other");

        const msg = document.createElement("div");
        msg.className = "msg";
        msg.dataset.id = idStr || "";
        if (m.clientTemp) msg.dataset.temp = "1"; // ✅ 임시표식

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

    const tempId = `tmp_${Date.now()}`;
    const nowTs  = Date.now();
    renderMessages([{ id: tempId, clientTemp: true, role: MY_ROLE, name: "나", text, ts: nowTs }]);
    composerEl.value = "";
    scrollToBottom();

    try {
        // 서버 전송
        const res = await apiPostMessage(text); // {id or chatIdx ...}
        const realId = String(res?.id ?? res?.chatIdx ?? "");

        if (realId) {
            const el = listEl.querySelector(`.msg[data-id="${CSS.escape(tempId)}"]`);
            if (el) {
                el.dataset.id = realId;
                el.removeAttribute('data-temp');
            }
            seenIds.add(realId);
        }

        // 4) 동기화
        await poll();
    } catch (err) {
        const el = listEl.querySelector(`.msg[data-id="${CSS.escape(tempId)}"]`);
        el?.closest('.row')?.remove();
        alert("전송 실패. 네트워크를 확인해주세요.");
        console.error(err);
    }
}


async function deleteMessage(messageId) {
    if (!confirm("이 메시지를 삭제하시겠습니까?")) return;
    try {
        const url = `/api/chat/${encodeURIComponent(MATCHING_ID)}/messages/${encodeURIComponent(messageId)}?meMemIdx=${encodeURIComponent(ME_MEM_IDX)}`;
        const r = await fetch(url, { method: "DELETE" });
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
