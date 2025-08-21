let LAWYERS = [];

const grid  = document.getElementById("grid");
const q     = document.getElementById("q");
const chips = document.getElementById("chips");
const sort  = document.getElementById("sort");
const empty = document.getElementById("empty");

const modal   = document.getElementById("modal");
const mTitle  = document.getElementById("mTitle");
const mBody   = document.getElementById("mBody");
const mClose  = document.getElementById("mClose");
const bookBtn = document.getElementById("bookBtn");
const matchForm = document.getElementById("matchForm");

let currentLawyer = null;

const USER_ID = Number(document.body.dataset.userId || 0);
if (!USER_ID) {
    alert("필수 파라미터(userId)가 없습니다. 다시 시도해주세요.");
    // 개발 단계: 홈으로 이동(혹은 로그인 페이지로)
    window.location.replace("/");
}

// ===== 칩/필터 =====
let ALL_SKILLS = []; // [{code:'DEFAMATION', label:'명예훼손'}, ...]
let activeSkill = null;

function uniqSkills(rows) {
    const pairs = new Map(); // code -> label
    rows.forEach(l => {
        (l.skillCodes || []).forEach((code, i) => {
            const label = (l.skills && l.skills[i]) || code;
            if (!pairs.has(code)) pairs.set(code, label);
        });
    });
    return [...pairs.entries()].map(([code, label]) => ({ code, label }));
}

function renderChips() {
    const all = [{ code: "", label: "전체" }, ...ALL_SKILLS.slice(0, 10)];
    chips.innerHTML = all.map(s =>
        `<button class="chip ${(!activeSkill && !s.code) || activeSkill === s.code ? 'is-active' : ''}" data-skill="${s.code}">${s.label}</button>`
    ).join("");
}

chips.addEventListener("click", (e) => {
    const btn = e.target.closest(".chip");
    if (!btn) return;
    const code = btn.dataset.skill || "";
    activeSkill = code || null;
    load(); // 서버에서 다시 불러오기
});

q.addEventListener("input", debounce(load, 250));
sort.addEventListener("change", load);

// ===== 정렬 =====
function compareBySort(a, b) {
    if (sort.value === "name") return (a.name || "").localeCompare(b.name || "");
    if (sort.value === "exp")  return (b.years || 0) - (a.years || 0); // null → 0
    return (b.id || 0) - (a.id || 0); // 최신 등록순
}

function sortRegisteredFirst(rows) {

    const avail = rows.filter(l => l.available !== false);
    const unavail = rows.filter(l => l.available === false);
    avail.sort(compareBySort);
    unavail.sort((a, b) => (b.id || 0) - (a.id || 0));
    return [...avail, ...unavail];
}

// ===== 카드 템플릿 =====
function cardHTML(l) {
    const bannerCls = (Number(l.id) % 2) ? "bg-a" : "bg-b";
    const tags = (l.skills || []).map(s => `<span class="tag">${s}</span>`).join("");
    return `
  <article class="card" role="listitem">
    <div class="card__banner ${bannerCls}"></div>
    <div class="card__body">
      <div class="avatar">👤</div>
      <div class="meta">
        <div class="name">${l.name} <small>${l.title || ''}</small>${l.years ? ` · <small>${l.years}년차</small>` : ''}</div>
        <div class="row"><span class="icon">📞</span><small>${l.phone || '-'}</small></div>
        <div class="row"><span class="icon">✉️</span><small>${l.email || '-'}</small></div>
        <div class="row"><span class="icon">📍</span><small>${l.addr || '-'}</small></div>
        <div class="tags">${tags}</div>
      </div>
    </div>
    <div class="card__footer">
      <button class="btn btn--accent act-inquiry" data-id="${l.id}">문의하기</button>
    </div>
  </article>`;
}

function placeholderHTML() {
    return `
  <article class="card is-disabled" role="listitem" aria-disabled="true">
    <div class="card__banner"></div>
    <div class="card__body">
      <div class="avatar">👤</div>
      <div class="meta placeholder">아직 미등록된 변호사입니다.</div>
    </div>
    <div class="card__footer">
      <button class="btn" disabled>문의하기</button>
    </div>
  </article>`;
}

// ===== 렌더 =====
function render() {
    const term = (q.value || "").trim().toLowerCase();

    let rows = LAWYERS.filter(l => {
        if (l.available === false) return true; // 자리채움 카드 유지
        if (term) {
            const blob = `${l.name||''} ${l.title||''} ${l.email||''} ${l.addr||''} ${(l.skills||[]).join(' ')}`.toLowerCase();
            if (!blob.includes(term)) return false;
        }
        return true;
    });

    rows = sortRegisteredFirst(rows);
    grid.innerHTML = rows.map(l => (l.available === false) ? placeholderHTML() : cardHTML(l)).join("");
    empty.hidden = rows.length !== 0;
}

// ===== 카드 클릭 → 모달 =====
grid.addEventListener("click", (e) => {
    const btn = e.target.closest(".act-inquiry");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const l = LAWYERS.find(x => x.id === id);
    if (!l) return;
    openModal(l);
});

function openModal(lawyer) {
    currentLawyer = lawyer;
    mTitle.textContent = `${lawyer.name} 변호사 상담 예약`;
    mBody.innerHTML = `
    <p><strong>${lawyer.name}</strong> · ${lawyer.title || ''}${lawyer.years ? ` (${lawyer.years}년차)` : ""}</p>
    <p class="muted">상담 예약을 진행합니다.</p>
  `;
    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
}
function closeModal() {
    modal.classList.remove("is-open");
    modal.setAttribute("aria-hidden", "true");
    currentLawyer = null;
}
mClose.addEventListener("click", closeModal);
modal.addEventListener("click", (e) => { if (e.target.classList.contains("modal__backdrop")) closeModal(); });

// ===== 상담 예약(매칭 요청) 제출 =====
bookBtn.addEventListener("click", () => {
    if (!currentLawyer) return;

    matchForm.userId.value = USER_ID;

    matchForm.expertId.value = currentLawyer.id;

    matchForm.submit();
});

// ===== 서버 연동 =====
async function load() {
    const params = new URLSearchParams();
    const term = (q.value || '').trim();
    if (term) params.set('q', term);
    if (activeSkill) params.set('skill', activeSkill); // enum 코드
    params.set('sort', sort.value || 'rec');

    try {
        const res = await fetch('/api/experts?' + params.toString(), { headers: { 'Accept': 'application/json' }});
        if (!res.ok) throw new Error('HTTP ' + res.status);
        LAWYERS = await res.json();
        ALL_SKILLS = uniqSkills(LAWYERS);
    } catch (e) {
        console.error(e);
        LAWYERS = [];
        ALL_SKILLS = [];
    }
    renderChips();
    render();
}

function debounce(fn, ms) {
    let t; return (...a) => { clearTimeout(t); t = setTimeout(()=>fn(...a), ms); };
}

// 초기 로드
renderChips();
load();
