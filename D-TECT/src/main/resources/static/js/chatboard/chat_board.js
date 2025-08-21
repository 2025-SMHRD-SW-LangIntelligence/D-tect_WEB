/* ========= 기본 설정 ========= */
const REFRESH_MS = 5000;

// URL 파라미터 (thread=스레드ID&me=user|expert)
const params = new URLSearchParams(location.search);
const THREAD_ID = params.get("thread") || "demo-thread-1";
const MY_ROLE = (params.get("me") || "user").toLowerCase(); // 'user' | 'expert'

// DOM
const listEl = document.getElementById("messageList");
const composerEl = document.getElementById("composer");
const sendBtn = document.getElementById("sendBtn");
const lastUpdEl = document.getElementById("lastUpdated");
const refreshBtn = document.getElementById("refreshBtn");
const jumpNewBtn = document.getElementById("jumpNew");
const fileListEl = document.getElementById("fileList");
const fileInput = document.getElementById("fileInput");
const uploadBtn = document.getElementById("uploadBtn");

// 상태
let lastId = 0;                 // 가장 최근 서버 메시지 id
let pollingTimer = null;
let pendingPoll = false;
let unseenCount = 0;
let seenIds = new Set();        // 중복 렌더 방지
let composing = false;          // IME(한글) 입력 중 플래그

/* ========= 유틸 ========= */
const fmt = (ts) => new Date(ts).toLocaleString("ko-KR", { hour12: false });
const nearBottom = () => (listEl.scrollHeight - listEl.scrollTop - listEl.clientHeight) < 80;
const scrollToBottom = () => (listEl.scrollTop = listEl.scrollHeight);
function escapeHtml(s) { return (s || "").replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m])); }

/* ========= 메시지 렌더 ========= */
function renderMessages(items) {
    if (!items?.length) return;
    const atBottom = nearBottom();

    const frag = document.createDocumentFragment();
    for (const m of items) {
        const idStr = String(m.id ?? "");
        // 서버 메시지 중복 방지 (temp id는 'tmp_'로 들어오므로 통과)
        if (idStr && !m.clientTemp && seenIds.has(idStr)) continue;
        if (idStr && !m.clientTemp) seenIds.add(idStr);

        lastId = Math.max(lastId, Number(m.id) || lastId);

        const row = document.createElement("div");
        row.className = "row " + (m.role === MY_ROLE ? "me" : "other");

        const msg = document.createElement("div");
        msg.className = "msg";
        msg.dataset.id = idStr || "";
        if (m.clientTemp) msg.dataset.temp = "1";

        const isMine = m.role === MY_ROLE;
        const delBtn = isMine ? `<button class="del-btn" data-id="${escapeHtml(idStr)}" aria-label="메시지 삭제">×</button>` : "";

        msg.innerHTML = `
      <div class="meta">
        <span class="badge">${m.role === "expert" ? "변호사" : "사용자"}</span>
        <span class="name">${escapeHtml(m.name || (m.role === "expert" ? "전문가" : "사용자"))}</span>
        <span class="time">${fmt(m.ts || Date.now())}</span>
      </div>
      <div class="body">${escapeHtml(m.text || "")}</div>
      ${delBtn}
    `;
        row.appendChild(msg);
        frag.appendChild(row);
    }

    listEl.appendChild(frag);

    if (atBottom) { scrollToBottom(); }
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
        const res = await fetch(`/api/chat/threads/${encodeURIComponent(THREAD_ID)}/messages?since=${lastId}`);
        if (!res.ok) throw new Error("메시지 조회 실패");
        const data = await res.json(); // [{id, role, name, text, ts}]
        renderMessages(data);

        const fRes = await fetch(`/api/chat/threads/${encodeURIComponent(THREAD_ID)}/files`);
        if (fRes.ok) { renderFiles(await fRes.json()); }

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

/* ========= 전송 ========= */
async function sendMessage() {
    const text = composerEl.value.trim();
    if (!text) return;

    // 임시 메시지(낙관적 UI)
    const tempId = `tmp_${Date.now()}`;
    renderMessages([{ id: tempId, clientTemp: true, role: MY_ROLE, name: "나", text, ts: Date.now() }]);
    composerEl.value = "";
    scrollToBottom();

    try {
        const res = await fetch(`/api/chat/threads/${encodeURIComponent(THREAD_ID)}/messages`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text, role: MY_ROLE })
        });

        // 서버가 새 id를 반환하면 temp → real id 치환
        if (res.ok) {
            const body = await res.json().catch(() => ({}));
            if (body?.id) {
                const tempEl = listEl.querySelector(`.msg[data-id="${CSS.escape(tempId)}"]`);
                if (tempEl) {
                    tempEl.dataset.id = String(body.id);
                    delete tempEl.dataset.temp;
                    seenIds.add(String(body.id));
                }
            }
        }
    } catch (err) {
        alert("전송 실패. 네트워크를 확인해주세요.");
        console.error(err);
    }
}

/* ========= 삭제 ========= */
async function deleteMessage(messageId, isTemp) {
    if (!confirm("이 메시지를 삭제하시겠습니까?")) return;

    // 임시 메시지는 프론트에서만 제거
    if (isTemp) {
        removeMessageEl(messageId);
        return;
    }

    try {
        const res = await fetch(`/api/chat/threads/${encodeURIComponent(THREAD_ID)}/messages/${encodeURIComponent(messageId)}`, {
            method: "DELETE"
        });
        if (!res.ok) throw new Error("삭제 실패");
        removeMessageEl(messageId);
    } catch (err) {
        alert("삭제에 실패했습니다.");
        console.error(err);
    }
}

function removeMessageEl(id) {
    const el = listEl.querySelector(`.msg[data-id="${CSS.escape(String(id))}"]`);
    if (el) {
        const row = el.closest(".row");
        if (row) row.remove();
    }
}

/* ========= 이벤트 ========= */
// 버튼 클릭 전송
sendBtn.addEventListener("click", sendMessage);

// Enter = 전송 (Shift+Enter는 줄바꿈, 한글 조합 중 제외)
composerEl.addEventListener("compositionstart", () => composing = true);
composerEl.addEventListener("compositionend", () => composing = false);
composerEl.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey && !composing) {
        e.preventDefault();
        sendMessage();
    }
});

// 메시지 목록에서 X(삭제) 클릭
listEl.addEventListener("click", (e) => {
    const btn = e.target.closest(".del-btn");
    if (!btn) return;
    const msg = btn.closest(".msg");
    if (!msg) return;

    const id = msg.dataset.id;
    const isTemp = msg.dataset.temp === "1";
    deleteMessage(id, isTemp);
});

// 수동 새로고침 & 새 글 이동
refreshBtn.addEventListener("click", poll);
jumpNewBtn.addEventListener("click", () => {
    unseenCount = 0;
    jumpNewBtn.hidden = true;
    scrollToBottom();
});

// 파일 업로드
uploadBtn.addEventListener("click", () => fileInput.click());
fileInput.addEventListener("change", async () => {
    const file = fileInput.files?.[0];
    if (!file) return;
    const fd = new FormData();
    fd.append("file", file);
    fd.append("role", MY_ROLE);

    try {
        await fetch(`/api/chat/threads/${encodeURIComponent(THREAD_ID)}/files`, {
            method: "POST",
            body: fd
        });
        fileInput.value = "";
        poll(); // 즉시 갱신
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
    await poll();       // 최초 로드
    startPoll
});

