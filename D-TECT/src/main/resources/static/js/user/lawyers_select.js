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
const searchBtn = document.getElementById("searchBtn"); // [수정]

let currentLawyer = null;

const USER_ID = Number(document.body.dataset.userId || 0);
if (!USER_ID) {
    alert("필수 파라미터(userId)가 없습니다. 다시 시도해주세요.");
    window.location.replace("/");
}

// 전문분야 코드-라벨 매핑
const SKILL_LABELS = {
    VIOLENCE:  "폭력",
    DEFAMATION:"명예훼손",
    STALKING:  "스토킹",
    SEXUAL:    "성범죄",
    LEAK:      "정보유출",
    BULLYING:  "따돌림·집단괴롭힘",
    CHANTAGE:  "협박·갈취",
    EXTORTION: "공갈·갈취"
};
const LABEL_TO_CODE = Object.fromEntries(
    Object.entries(SKILL_LABELS).map(([code, label]) => [label.toLowerCase(), code])
);

// ===== 칩/필터 =====
let ALL_SKILLS = []; // [{code:'DEFAMATION', label:'명예훼손'}, ...]
let activeSkill = null;

function uniqSkills(rows) {
    const pairs = new Map(); // code -> label
    rows.forEach(l => {
        (l.skillCodes || []).forEach((code, i) => {
            const label = (l.skills && l.skills[i]) || SKILL_LABELS[code] || code; // [수정]
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
    q.value = "";
    load();
});

// 검색 버튼 클릭/Enter에만 검색 수행
searchBtn?.addEventListener("click", () => {
    const term = (q.value || "").trim();
    // 검색어가 전문분야(한글/영문)과 '부분 일치'하면 그 분야로 필터
    const skillCode = toSkillCode(term);
    activeSkill = skillCode || null;
    load();
});
q.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        e.preventDefault();
        searchBtn?.click();
    }
});

sort.addEventListener("change", load);

// ===== 이름 정렬
function compareBySort(a, b) {
    if (sort.value === "name") {
        const an = (a.name || "").trim();
        const bn = (b.name || "").trim();
        return an.localeCompare(bn, "ko-KR", { sensitivity: "base", numeric: true });
    }
    if (sort.value === "exp")  return (b.years || 0) - (a.years || 0);
    return (b.id || 0) - (a.id || 0);
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
            // 부분 일치 고려: 한글 라벨 + 영문 코드 + 기타 메타
            const blob = [
                l.name || '',
                l.title || '',
                l.email || '',
                l.addr || '',
                ...(l.skills || []),     // 한글 라벨
                ...(l.skillCodes || [])  // 영문 코드
            ].join(' ').toLowerCase();

            if (!blob.includes(term)) return false; // 부분 일치 [수정]
        }
        return true;
    });

    rows = sortRegisteredFirst(rows);
    grid.innerHTML = rows.map(l => (l.available === false) ? placeholderHTML() : cardHTML(l)).join("");
    empty.hidden = rows.length !== 0;

    renderChips();
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
    window.location.href = `/matching/inquiry?userId=${USER_ID}&expertId=${currentLawyer.id}`;
});

// ===== [수정] 검색어 → 전문분야 코드 변환(부분 일치 지원)
function toSkillCode(termRaw) {
    if (!termRaw) return null;
    const t = termRaw.trim().toLowerCase();

    // 1) 영문 코드 정확 일치 (ex. leak, defamation)
    const codeExactEng = Object.keys(SKILL_LABELS).find(c => c.toLowerCase() === t);
    if (codeExactEng) return codeExactEng;

    // 2) 한글 라벨 정확 일치 (ex. 정보유출)
    if (LABEL_TO_CODE[t]) return LABEL_TO_CODE[t];

    // 3) 한글 라벨 '부분 일치' (ex. "정보" -> "정보유출")
    const partial = Object.entries(SKILL_LABELS)
        .filter(([_, label]) => label.toLowerCase().includes(t))
        .map(([code]) => code);

    // 유일하게 매칭되면 채택, 다수면 모호하므로 일반 검색으로 처리
    return partial.length === 1 ? partial[0] : null; // [수정]
}

// ===== 서버 연동 =====
async function load() {
    const params = new URLSearchParams();

    // skill 우선: activeSkill이 있으면 서버에 skill만 전달
    if (activeSkill) params.set('skill', activeSkill);

    // 일반 텍스트 검색은 클라이언트에서 부분 일치 필터링으로 처리

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
    render();
}

// 디바운스 유틸(현재 미사용, 남겨둠)
function debounce(fn, ms) {
    let t; return (...a) => { clearTimeout(t); t = setTimeout(()=>fn(...a), ms); };
}

// 초기 로드
renderChips();
load();
