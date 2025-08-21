// ===== 더미 데이터 (API 연동 시 대체) =====
let LAWYERS = [
    {
        id: 1, name: "고○○", title: "변호사", years: 7,
        phone: "010-1234-4321", email: "goosuntone@gmail.com", addr: "스마트 리젠 개발길 903호",
        skills: ["명예훼손", "성범죄", "영업비밀", "성모욕"],
        banner: "a", available: true
    },
    {
        id: 2, name: "정○○", title: "변호사", years: 10,
        phone: "010-5678-9876", email: "rightbefore@gmail.com", addr: "스마트 리젠 개발길 908호",
        skills: ["영업비밀", "전문분쟁", "산업재산권", "산업재해"],
        banner: "b", available: true
    },
    // 미등록/자리 채우기
    { id: 3, available: false },
    { id: 4, available: false },
];

// ===== 상태 =====
const grid = document.getElementById("grid");
const q = document.getElementById("q");
const chips = document.getElementById("chips");
const sort = document.getElementById("sort");
const empty = document.getElementById("empty");

// 모달 상태
const modal = document.getElementById("modal");
const mTitle = document.getElementById("mTitle");
const mBody = document.getElementById("mBody");
const mClose = document.getElementById("mClose");
const bookBtn = document.getElementById("bookBtn");
let currentLawyer = null;

// 분야 칩
const ALL_SKILLS = [...new Set(LAWYERS.flatMap(l => l.skills || []))].slice(0, 10);
let activeSkill = null;

function renderChips() {
    chips.innerHTML = [
        `<button class="chip ${!activeSkill ? 'is-active' : ''}" data-skill="">전체</button>`,
        ...ALL_SKILLS.map(s => `<button class="chip ${activeSkill === s ? 'is-active' : ''}" data-skill="${s}">${s}</button>`)
    ].join("");
}
renderChips();

chips.addEventListener("click", (e) => {
    const btn = e.target.closest(".chip");
    if (!btn) return;
    activeSkill = btn.dataset.skill || null;
    renderChips();
    render();
});

// 검색/정렬 이벤트
q.addEventListener("input", render);
sort.addEventListener("change", render);

// 선택된 정렬 기준 비교 함수
function compareBySort(a, b) {
    if (sort.value === "name") return (a.name || "").localeCompare(b.name || "");
    if (sort.value === "exp") return (b.years || 0) - (a.years || 0);
    return (b.id || 0) - (a.id || 0); // 최신 등록순
}

// ✅ 등록(available) 우선 → 미등록은 항상 맨 아래
function sortRegisteredFirst(rows) {
    const avail = rows.filter(l => l.available);
    const unavail = rows.filter(l => !l.available);
    avail.sort(compareBySort);
    // 미등록 카드도 필요하면 고유 순서 유지 or 보조정렬
    unavail.sort((a, b) => (b.id || 0) - (a.id || 0));
    return [...avail, ...unavail];
}

// 카드 렌더
function render() {
    const term = (q.value || "").trim().toLowerCase();

    let rows = LAWYERS.filter(l => {
        if (!l.available) return true; // 미등록 카드는 항상 표시 (자리 채움)
        if (activeSkill && !(l.skills || []).includes(activeSkill)) return false;
        if (term) {
            const blob = `${l.name || ''} ${l.title || ''} ${l.email || ''} ${l.addr || ''} ${(l.skills || []).join(' ')}`.toLowerCase();
            if (!blob.includes(term)) return false;
        }
        return true;
    });

    rows = sortRegisteredFirst(rows);  // ✅ 핵심 정렬

    grid.innerHTML = rows.map(l => l.available ? cardHTML(l) : placeholderHTML()).join("");
    empty.hidden = rows.length !== 0;
}

function cardHTML(l) {
    const bannerCls = l.banner === "b" ? "bg-b" : "bg-a";
    return `
  <article class="card" role="listitem">
    <div class="card__banner ${bannerCls}"></div>
    <div class="card__body">
      <div class="avatar">👤</div>
      <div class="meta">
        <div class="name">${l.name} <small>${l.title}</small>${l.years ? ` · <small>${l.years}년차</small>` : ''}</div>
        <div class="row"><span class="icon">📞</span><small>${l.phone}</small></div>
        <div class="row"><span class="icon">✉️</span><small>${l.email}</small></div>
        <div class="row"><span class="icon">📍</span><small>${l.addr}</small></div>
        <div class="tags">${(l.skills || []).map(s => `<span class="tag">${s}</span>`).join("")}</div>
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

grid.addEventListener("click", (e) => {
    const btn = e.target.closest(".act-inquiry");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const l = LAWYERS.find(x => x.id === id);
    if (!l) return;
    openModal(l);
});

// 모달
function openModal(lawyer) {
    currentLawyer = lawyer;
    mTitle.textContent = `${lawyer.name} 변호사 상담 예약`;
    mBody.innerHTML = `
    <p><strong>${lawyer.name}</strong> · ${lawyer.title}${lawyer.years ? ` (${lawyer.years}년차)` : ""}</p>
    <p class="muted">상담 예약 페이지로 이동합니다.</p>
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

// ✅ 상담 예약만 이동
bookBtn.addEventListener("click", () => {
    if (!currentLawyer) return;
    window.location.href = `/reservation?lawyerId=${currentLawyer.id}`;
});

// 초기 표시
render();
