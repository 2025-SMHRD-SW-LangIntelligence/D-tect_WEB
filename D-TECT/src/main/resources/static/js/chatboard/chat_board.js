// 채팅방 프런트 (mem 파라미터 기반)
// URL 예) /chat/room/5?me=expert&mem=12
const REFRESH_MS = 1000;

/* ====== 옵션: 아이콘/텍스트 버튼 전환 ======
   true  -> 아이콘형(작고 정사각형)
   false -> 텍스트형(‘다운로드’ 글자 표시)
================================================ */
const USE_ICON_BUTTON = true;

/* ========= 파라미터 ========= */
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
const ME_MEM_IDX  = resolveMyMemIdx();   // meMemIdx

if (!MATCHING_ID) alert("유효하지 않은 채팅방 주소입니다. (matchingId 없음)");

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
// 파일 시간: "YYYY.MM.DD HH:mm"
const fmtHM = (ts) => {
    const d = new Date(ts);
    if (Number.isNaN(d.getTime())) return "-";
    const y  = d.getFullYear();
    const m  = String(d.getMonth()+1).padStart(2,"0");
    const da = String(d.getDate()).padStart(2,"0");
    const hh = String(d.getHours()).padStart(2,"0");
    const mm = String(d.getMinutes()).padStart(2,"0");
    return `${y}.${m}.${da} ${hh}:${mm}`;
};

const nearBottom = () => (listEl.scrollHeight - listEl.scrollTop - listEl.clientHeight) < 80;
const scrollToBottom = () => (listEl.scrollTop = listEl.scrollHeight);
function escapeHtml(s){
    return (s || "").replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
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
        name: m.senderName || (String(m.senderType || "").toUpperCase() === "EXPERT" ? "전문가" : "사용자"),
        text: m.chatContent ?? m.content ?? m.text ?? "",
        ts:   m.chatedAt ?? m.createdAt ?? m.ts ?? Date.now(),
        file: m.sendedFile || m.file || null
    }));
}

async function apiPostMessage(text) {
    const fd = new FormData();
    fd.append("meMemIdx", String(ME_MEM_IDX));
    fd.append("content", text);
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
        const raw = await res.json();

        // 1) 파일 단위(flat)
        if (Array.isArray(raw) && raw.length && (raw[0].id || raw[0].name || raw[0].fileIdx || raw[0].fileName)) {
            return raw.map(f => {
                const rawRole = (f.by && f.by.role) || f.senderType || f.uploaderType || f.role || "";
                const roleUp  = String(rawRole).toUpperCase();
                const role    = roleUp === "EXPERT" ? "expert" : roleUp === "USER" ? "user" : null;
                return {
                    id:   f.id ?? f.fileIdx,
                    url:  f.url ?? (`/api/chat/file/${f.id ?? f.fileIdx}`),
                    ts:   f.ts ?? f.createdAt ?? Date.now(),
                    name: f.fileName ?? f.name ?? "첨부",
                    role
                };
            });
        }

        // 2) 업로드 단위(중첩) → 평탄화
        const flat = [];
        (raw || []).forEach(u => {
            const uploadAt  = u.ts ?? u.createdAt ?? Date.now();
            const upRoleRaw = (u.by && u.by.role) || u.uploaderType || u.senderType || u.role || "";
            const upRoleUp  = String(upRoleRaw).toUpperCase();

            (u.uploadFileList || u.files || []).forEach(f => {
                const fileRoleRaw = (f.by && f.by.role) || f.uploaderType || f.senderType || f.role || upRoleUp;
                const fileRoleUp  = String(fileRoleRaw).toUpperCase();
                const role        = fileRoleUp === "EXPERT" ? "expert" : fileRoleUp === "USER" ? "user" : null;

                flat.push({
                    id:   f.id ?? f.fileIdx,
                    url:  f.url ?? (`/api/chat/file/${f.id ?? f.fileIdx}`),
                    ts:   f.ts ?? f.createdAt ?? uploadAt,
                    name: f.fileName ?? f.name ?? "첨부",
                    role
                });
            });
        });
        return flat;
    } catch {
        return [];
    }
}

async function apiUploadFiles(fileList) {
    const fd = new FormData();
    Array.from(fileList).forEach(f => fd.append("file", f)); // key = "file"
    fd.append("meMemIdx", String(ME_MEM_IDX));
    const url = `/api/chat/${encodeURIComponent(MATCHING_ID)}/files`;
    const r = await fetch(url, { method: "POST", body: fd });
    if (!r.ok) throw new Error("파일 업로드 실패");
    return await r.json().catch(() => ([]));
}

/* ========= 메시지 렌더 ========= */
function renderMessages(items) {
    if (!items?.length) return;
    const atBottom = nearBottom();

    const frag = document.createDocumentFragment();
    for (const m of items) {
        const idStr = String(m.id ?? "");

        // 임시 → 실제 ID 치환
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
                continue;
            }
        }

        if (idStr && seenIds.has(idStr)) continue;
        if (idStr) seenIds.add(idStr);

        const row = document.createElement("div");
        row.className = "row " + (m.role === MY_ROLE ? "me" : "other");

        const msg = document.createElement("div");
        msg.className = "msg";
        msg.dataset.id = idStr || "";

        // 메시지 내 첨부도 아이콘 버튼 재사용
        const icon = `
      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none">
        <path d="M12 3v12m0 0 5-5m-5 5-5-5M5 21h14"
              stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>`;
        const fileLink = m.file
            ? `<div class="file"><a class="dl-btn" href="${escapeHtml(m.file)}" download aria-label="다운로드" title="다운로드">${icon}</a></div>`
            : "";

        msg.innerHTML = `
      <div class="meta">
        <span class="badge">${m.role === "expert" ? "법률 전문가" : "사용자"}</span>
        <span class="name">${escapeHtml(m.name || (m.role === "expert" ? "전문가" : "사용자"))}</span>
        <span class="time">${fmt(m.ts || Date.now())}</span>
      </div>
      <div class="body">${escapeHtml(m.text || "")}</div>
      ${fileLink}
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

/* ========= 파일 렌더 ========= */
function renderFiles(items){
    const icon = `
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none">
      <path d="M12 3v12m0 0 5-5m-5 5-5-5M5 21h14"
            stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>`;

    fileListEl.innerHTML = (items || []).map(f => {
        const rawRole = (f?.by?.role ?? f.role ?? "").toString().toLowerCase();
        const role    = rawRole === "expert" ? "expert" : rawRole === "user" ? "user" : "neutral";
        const roleText= role === "expert" ? "전문가" : role === "user" ? "사용자" : "첨부";
        const nameSafe= escapeHtml(f.name || "첨부");
        const urlSafe = escapeHtml(f.url);

        const btnHtml = USE_ICON_BUTTON
            ? `<a class="dl-btn" href="${urlSafe}" download aria-label="다운로드" title="다운로드">${icon}</a>`
            : `<a class="dl-btn" style="width:88px;height:34px;padding:0 10px;">다운로드</a>`;

        return `
      <li class="file-row">
        <span class="role-badge role--${role}">${roleText}</span>
        <div class="ftext">
          <div class="fname" title="${nameSafe}">${nameSafe}</div>
          <div class="ftime">${fmtHM(f.ts)}</div>
        </div>
        ${btnHtml}
      </li>
    `;
    }).join("");
}

/* ========= 폴링 ========= */
async function poll() {
    if (pendingPoll) return;
    pendingPoll = true;
    listEl.setAttribute("aria-busy", "true");
    try {
        const msgs  = await apiListMessages();
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

/* ========= 전송 ========= */
async function sendMessage() {
    const text = (composerEl.value || "").trim();
    if (!text) return;

    const tempId = `tmp_${Date.now()}`;
    const nowTs  = Date.now();
    renderMessages([{ id: tempId, clientTemp: true, role: MY_ROLE, name: "나", text, ts: nowTs }]);
    composerEl.value = "";
    scrollToBottom();

    try {
        const res = await apiPostMessage(text);
        const realId = String(res?.id ?? res?.chatIdx ?? "");
        if (realId) {
            const el = listEl.querySelector(`.msg[data-id="${CSS.escape(tempId)}"]`);
            if (el) {
                el.dataset.id = realId;
                el.removeAttribute('data-temp');
            }
            seenIds.add(realId);
        }
        await poll();
    } catch (err) {
        const el = listEl.querySelector(`.msg[data-id="${CSS.escape(tempId)}"]`);
        el?.closest('.row')?.remove();
        alert("전송 실패. 네트워크를 확인해주세요.");
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
refreshBtn.addEventListener("click", poll);
jumpNewBtn.addEventListener("click", () => { unseenCount = 0; jumpNewBtn.hidden = true; scrollToBottom(); });

uploadBtn.addEventListener("click", () => fileInput.click());
fileInput.addEventListener("change", async () => {
    const files = fileInput.files;
    if (!files || files.length === 0) return;
    try {
        await apiUploadFiles(files);
        fileInput.value = "";
        await poll();
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
