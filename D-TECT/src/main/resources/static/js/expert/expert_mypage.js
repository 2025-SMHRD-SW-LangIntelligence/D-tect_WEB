import { initProfileEditPopup } from '/js/public/common.js';

const PAGE_SIZE = 5;
let page = 1;
let allItems = [];

// DOM
const listEl   = document.getElementById('applyList');
const pageInfo = document.getElementById('pageInfo');
const prevBtn  = document.getElementById('prevPage');
const nextBtn  = document.getElementById('nextPage');

const caseSelect = document.getElementById('caseSelect');
const feeForm    = document.getElementById('feeForm');
const feeInput   = document.getElementById('fee');
const memoInput  = document.getElementById('memo');

const expertId = Number(document.body.dataset.expertId || 0);
const myMemIdx = Number(document.body.dataset.memIdx || 0);

// 공통 팝업 호출 (전문분야 다중 선택)
initProfileEditPopup('#editToggleBtn', {
  specialtyOptions: window.__SPECIALTIES__ || [],
});

// ===== 코드 → 한글 라벨 =====
const REASON_LABELS = {
  VIOLENCE:"폭력", DEFAMATION:"명예훼손", STALKING:"스토킹", SEXUAL:"성범죄",
  LEAK:"정보유출", BULLYING:"따돌림·집단괴롭힘", CHANTAGE:"협박·갈취", EXTORTION:"공갈·갈취",
};
const mapReason = v => v ? (REASON_LABELS[String(v).trim().toUpperCase()] || v) : '—';

function fmt(ts){
  if (!ts) return '—';
  const d = new Date(ts); if (Number.isNaN(d.getTime())) return '—';
  return d.toISOString().slice(0,10);
}
function statusKorean(s){
  switch(String(s||'').toUpperCase()){
    case 'APPROVED': return '승인';
    case 'REJECTED': return '반려';
    case 'PENDING':  return '대기';
    case 'COMPLETED':return '완료';
    case 'CANCELED': return '취소';
    default:         return '—';
  }
}
function badgeClassKor(k){
  if (k==='승인'||k==='완료') return 'badge badge--ok';
  if (k==='반려'||k==='취소') return 'badge badge--danger';
  return 'badge badge--warn';
}
function chatEnabledByStatus(enumU){ return enumU==='APPROVED' || enumU==='COMPLETED'; }

// ===== 상태 메뉴(포털) =====
const PORTAL_ID = 'status-menu-portal';
let openContext = null; // {id, current, anchor}

function ensurePortal(){
  let el = document.getElementById(PORTAL_ID);
  if (el) return el;
  el = document.createElement('div');
  el.id = PORTAL_ID;
  el.className = 'status-menu-portal';
  el.hidden = true;
  document.body.appendChild(el);
  return el;
}
const ALL_NEXT = ['APPROVED','REJECTED','COMPLETED','CANCELED'];
const LABELS   = { APPROVED:'승인', REJECTED:'반려', COMPLETED:'완료', CANCELED:'취소' };

function openStatusMenu(anchorBtn, id, currentEnum){
  const portal = ensurePortal();
  // 선택지: PENDING 제외 + 현재값 제외
  const options = ALL_NEXT.filter(s => s !== currentEnum);
  portal.innerHTML = options.length
      ? options.map(s => `<button class="status-option" data-next="${s}" data-id="${id}">${LABELS[s]}</button>`).join('')
      : `<div class="status-empty">변경 불가</div>`;

  // 위치 잡기 (뷰포트 기준)
  const r = anchorBtn.getBoundingClientRect();
  portal.style.left = `${Math.round(r.left)}px`;
  portal.style.top  = `${Math.round(r.bottom + 6)}px`;
  portal.hidden = false;

  openContext = { id, current: currentEnum, anchor: anchorBtn };
}

function closeStatusMenu(){
  const portal = document.getElementById(PORTAL_ID);
  if (portal) portal.hidden = true;
  openContext = null;
}

// 전역 클릭: 트리거/메뉴 외 클릭 시 닫기
document.addEventListener('click', (e)=>{
  const trigger = e.target.closest('.status-trigger');
  if (trigger){
    const id = Number(trigger.dataset.id);
    const current = String(trigger.dataset.status || '').toUpperCase();
    // 토글
    if (openContext && openContext.anchor === trigger){
      closeStatusMenu(); return;
    }
    openStatusMenu(trigger, id, current);
    return;
  }
  // 메뉴 내부 클릭은 여기 말고 아래 핸들러에서 처리
  if (!e.target.closest(`#${PORTAL_ID}`)) closeStatusMenu();
});
window.addEventListener('scroll', closeStatusMenu, true);
window.addEventListener('resize', closeStatusMenu);

// 메뉴 항목 클릭
document.addEventListener('click', (e)=>{
  const opt = e.target.closest(`#${PORTAL_ID} .status-option`);
  if (!opt) return;
  const id   = Number(opt.dataset.id);
  const next = String(opt.dataset.next || '').toUpperCase();
  updateStatus(id, next);
  closeStatusMenu();
});

// ===== 행 렌더 =====
function rowTemplate(item){
  const requestedAt = fmt(item.requestedAt);
  const matchedAt   = fmt(item.matchedAt);
  const customer    = item.customerName ?? '—';
  const reason      = mapReason(item.reasonLabel || item.requestReason || '');

  const statusEnum  = String(item.status || '').toUpperCase();
  const sKor        = statusKorean(statusEnum);
  const enabled     = chatEnabledByStatus(statusEnum);
  const chatUrl     = item.chatUrl || `/chat/room/${item.matchingIdx}?me=expert&mem=${myMemIdx}`;

  return `
    <li class="list-row" role="row">
      <div class="col">${requestedAt}</div>
      <div class="col">${customer}</div>
      <div class="col">${reason}</div>
      <div class="col">${matchedAt}</div>
      <div class="col">
        <button type="button"
                class="status-trigger ${badgeClassKor(sKor)}"
                data-id="${item.matchingIdx}"
                data-status="${statusEnum}">
          ${sKor}<span class="caret" aria-hidden="true">▾</span>
        </button>
      </div>
      <div class="col"><button class="chat-btn" data-url="${chatUrl}" ${enabled ? '' : 'disabled'}>입장하기</button></div>
    </li>`;
}

// 채팅 버튼
listEl?.addEventListener('click', (e) => {
  const btn = e.target.closest('.chat-btn');
  if (!btn || btn.disabled) return;
  const url = btn.dataset.url || '#';
  if (url !== '#') window.location.href = url;
});

// ===== 상태 변경 호출 =====
async function updateStatus(id, next){
  if (next === 'PENDING') { alert('PENDING 으로는 변경할 수 없습니다.'); return; }
  const ep = {
    APPROVED:  `/api/matching/${id}/approve`,
    REJECTED:  `/api/matching/${id}/reject`,
    COMPLETED: `/api/matching/${id}/complete`,
    CANCELED:  `/api/matching/${id}/cancel`,
  }[next];
  if (!ep){ alert('알 수 없는 상태입니다.'); return; }

  try{
    const res = await fetch(ep, { method:'POST', headers:{ 'Accept':'application/json' }});
    if (!res.ok) throw new Error(await res.text() || `HTTP ${res.status}`);
    await loadData(); // 갱신
  }catch(err){
    console.error(err);
    alert('상태 변경 실패: ' + err.message);
  }
}

// ===== 목록 렌더 & 페이지네이션 =====
function renderList(){
  const total = allItems.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  page = Math.min(Math.max(1,page), totalPages);
  if (pageInfo) pageInfo.textContent = `${page} / ${totalPages}`;

  const start = (page - 1) * PAGE_SIZE;
  const items = allItems.slice(start, start + PAGE_SIZE);

  if (!items.length){
    listEl.innerHTML = `<li class="list-row">
      <div class="col" style="grid-column:1/7;text-align:center;color:#888">데이터가 없습니다.</div>
    </li>`;
  }else{
    listEl.innerHTML = items.map(rowTemplate).join('');
  }
  hydrateCaseSelect(items);
  if (prevBtn) prevBtn.disabled = page <= 1;
  if (nextBtn) nextBtn.disabled = page >= totalPages;
}
prevBtn?.addEventListener('click', ()=>{ page--; renderList(); });
nextBtn?.addEventListener('click', ()=>{ page++; renderList(); });

// ===== 상담료 케이스 셀렉트 =====
function hydrateCaseSelect(visibleItems){
  caseSelect.innerHTML = visibleItems.map(i=>{
    const reason = mapReason(i.reasonLabel || i.requestReason || '');
    return `<option value="${i.matchingIdx}">[${fmt(i.requestedAt)}] ${i.customerName ?? '—'} - ${reason}</option>`;
  }).join('');
}
feeForm?.addEventListener('submit', (e)=>{
  e.preventDefault();
  const id   = caseSelect.value;
  const memo = (memoInput.value || '').trim();
  const fee  = Number(feeInput.value || 0);
  if (!id){ alert('케이스를 선택하세요.'); return; }
  if (!(fee > 0)){ alert('상담료를 입력하세요.'); return; }
  console.log('상담료 저장', { caseId:id, fee, memo });
  alert('상담료가 저장되었습니다.');
  memoInput.value = ''; feeInput.value = '';
});

// ===== 데이터 로드 =====
async function loadData(){
  try{
    const id = Number(document.body.dataset.expertId || 0);
    if (!id) throw new Error('expertId가 바인딩되지 않았습니다.');
    const res = await fetch(`/mypage/api/expert/${id}/matchings`, { headers:{ 'Accept':'application/json' }});
    if (!res.ok) throw new Error(`API Error: ${res.status}`);
    const data = await res.json(); // ExpertMatchingSummaryDto[]
    allItems = Array.isArray(data)
        ? data.sort((a,b)=> new Date(b.requestedAt)-new Date(a.requestedAt))
        : [];
  }catch(e){
    console.error(e);
    allItems = [];
  }finally{
    renderList();
  }
}
loadData();

// ===== 상단 버튼 =====
document.getElementById('logoutBtn')?.addEventListener('click', ()=>{ window.location.href='/logout'; });
document.getElementById('reqBtn')?.addEventListener('click', ()=> alert('상담 신청 확인으로 이동'));
document.getElementById('scheduleBtn')?.addEventListener('click', ()=> alert('상담 일정 확인하기로 이동'));
document.getElementById('withdrawBtn')?.addEventListener('click', ()=>{
  if (confirm('정말로 회원을 탈퇴하시겠습니까?')) alert('탈퇴 처리');
});
