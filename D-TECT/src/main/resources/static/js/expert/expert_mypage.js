import { initProfileEditPopup } from '/js/public/common.js';

const PAGE_SIZE = 5;
let page = 1;
let allItems = [];

// DOM
const listEl   = document.getElementById('applyList');
const pageInfo = document.getElementById('pageInfo');
const prevBtn  = document.getElementById('prevPage');
const nextBtn  = document.getElementById('nextPage');
const expertId = Number(document.body.dataset.expertId || 0);
const myMemIdx = Number(document.body.dataset.memIdx || 0);

// 공통 팝업
initProfileEditPopup?.('#editToggleBtn', {
  specialtyOptions: window.__SPECIALTIES__ || []
});

// ===== 라벨/유틸 =====
const REASON_LABELS = {
  VIOLENCE:"폭력", DEFAMATION:"명예훼손", STALKING:"스토킹", SEXUAL:"성범죄",
  LEAK:"정보유출", BULLYING:"따돌림·집단괴롭힘", CHANTAGE:"협박·갈취", EXTORTION:"공갈·갈취",
};
const mapReason = v => (v && REASON_LABELS[String(v).trim().toUpperCase()]) || v || "—";
const fmt = ts => {
  if (!ts) return '—';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toISOString().slice(0,10);
};
function statusKorean(s){
  switch (String(s||'').toUpperCase()){
    case 'APPROVED': return '승인';
    case 'REJECTED': return '반려';
    case 'PENDING':  return '대기';
    case 'COMPLETED':return '완료';
    case 'CANCELED': return '취소';
    case 'PAID':     return '결제완료';
    default:         return '—';
  }
}
const badgeClassKor = k => (k==='승인'||k==='완료'||k==='결제완료') ? 'badge badge--ok'
    : (k==='반려'||k==='취소') ? 'badge badge--danger' : 'badge badge--warn';
const chatEnabledByStatus = s => s === 'APPROVED' || s === 'COMPLETED';

// ===== 상태 컨트롤 =====
const LABELS = { APPROVED:'승인', REJECTED:'반려', COMPLETED:'완료', CANCELED:'취소' };
const nextActions = () => ['APPROVED','REJECTED','COMPLETED','CANCELED'];

function renderStatusControl(item){
  const statusEnum = String(item.status || '').toUpperCase();
  const sKor = statusKorean(statusEnum);
  if (statusEnum === 'PAID') {
    return `<span class="badge badge--ok">결제완료</span>`;
  }
  const options = nextActions()
      .filter(s => s !== statusEnum)
      .map(s => `<button type="button" class="status-option" data-id="${item.matchingIdx}" data-next="${s}">${LABELS[s]}</button>`)
      .join('');
  return `
    <div class="status-wrap">
      <button type="button" class="status-btn ${badgeClassKor(sKor)}" data-open="1">
        ${sKor} <span class="caret" aria-hidden="true">▾</span>
      </button>
      <div class="status-menu" hidden>
        ${options || '<div class="status-empty">변경 불가</div>'}
      </div>
    </div>`;
}

// ===== 행 템플릿 =====
function rowTemplate(item){
  const requestedAt = fmt(item.requestedAt);
  const matchedAt   = fmt(item.matchedAt);
  const customer    = item.customerName ?? '—';
  const reason      = mapReason(item.reasonLabel || item.requestReason || '');
  const statusEnum  = String(item.status || '').toUpperCase();
  const enabledChat = chatEnabledByStatus(statusEnum);
  const chatUrl     = item.chatUrl || `/chat/room/${item.matchingIdx}?me=expert&mem=${myMemIdx}`;
  const canInvoice  = statusEnum === 'COMPLETED';

  return `
    <li class="list-row" role="row">
      <div class="col">${requestedAt}</div>
      <div class="col">${customer}</div>
      <div class="col">${reason}</div>
      <div class="col">${matchedAt}</div>
      <div class="col">${renderStatusControl(item)}</div>
      <div class="col">
        <button class="chat-btn" data-url="${chatUrl}" ${enabledChat ? '' : 'disabled'}>입장하기</button>
        <button class="invoice-btn" data-open-invoice data-matching-id="${item.matchingIdx}" data-user-name="${customer}" data-reason="${reason}" ${canInvoice ? '' : 'disabled'}>청구서</button>
      </div>
    </li>`;
}

// ===== 목록/페이지네이션 =====
function renderList(){
  const total = allItems.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  page = Math.min(Math.max(1,page), totalPages);
  pageInfo.textContent = `${page} / ${totalPages}`;

  const start=(page-1)*PAGE_SIZE;
  const items=allItems.slice(start, start+PAGE_SIZE);

  listEl.innerHTML = items.length
      ? items.map(rowTemplate).join('')
      : `<li class="list-row"><div class="col" style="grid-column:1/7;text-align:center;color:#888">데이터가 없습니다.</div></li>`;

  prevBtn && (prevBtn.disabled = page <= 1);
  nextBtn && (nextBtn.disabled = page >= totalPages);
}
prevBtn?.addEventListener('click', ()=>{ page--; renderList(); });
nextBtn?.addEventListener('click', ()=>{ page++; renderList(); });

// ===== 채팅방 입장하기 =====
listEl?.addEventListener('click', (e) => {
  const chat = e.target.closest('.chat-btn');
  if (!chat) return;
  e.preventDefault();
  if (chat.disabled) return;
  const url = chat.dataset.url || '#';
  if (url && url !== '#') location.href = url;
});

// ===== 드롭다운 (portal + auto flip) =====
let openMenu = null;
let ownerWrap = null;
function closeStatusMenu(){
  if (!openMenu) return;
  openMenu.hidden = true;
  openMenu.classList.remove('status-menu-portal');
  openMenu.style.cssText = '';
  ownerWrap?.appendChild(openMenu);
  openMenu = null; ownerWrap = null;
}
function openStatusMenuBeside(btn){
  const wrap = btn.closest('.status-wrap');
  const menu = wrap?.querySelector('.status-menu');
  if (!menu) return;
  if (openMenu === menu){ closeStatusMenu(); return; }
  closeStatusMenu();
  ownerWrap = wrap;
  menu.hidden = false;
  menu.classList.add('status-menu-portal');
  document.body.appendChild(menu);

  const b = btn.getBoundingClientRect();
  const vw = window.innerWidth;
  const vh = window.innerHeight;

  let left = Math.round(b.right + 8);
  let top  = Math.round(b.top + (b.height - menu.offsetHeight)/2);
  menu.style.left = `${left}px`;
  menu.style.top  = `${top}px`;

  const r = menu.getBoundingClientRect();
  if (r.right > vw - 8) {
    left = Math.max(8, Math.round(b.left - r.width - 8));
    menu.style.left = `${left}px`;
  }
  const r2 = menu.getBoundingClientRect();
  if (r2.top < 8) menu.style.top = `8px`;
  if (r2.bottom > vh - 8) menu.style.top = `${Math.max(8, vh - r2.height - 8)}px`;
  openMenu = menu;
}
// 열기/옵션/닫기
document.addEventListener('click', (e)=>{
  const btn = e.target.closest('.status-btn[data-open]');
  if (btn && listEl?.contains(btn)){ e.preventDefault(); openStatusMenuBeside(btn); }
});
document.addEventListener('click', (e)=>{
  const opt = e.target.closest('.status-option');
  if (!opt || !document.body.contains(opt)) return;
  const id = Number(opt.dataset.id);
  const next = String(opt.dataset.next || '').toUpperCase();
  closeStatusMenu();
  updateStatus(id, next);
});
window.addEventListener('click', (e)=>{
  if (e.target.closest('.status-wrap') || e.target.closest('.status-menu-portal')) return;
  closeStatusMenu();
});
window.addEventListener('scroll', closeStatusMenu, { passive:true });
window.addEventListener('resize', closeStatusMenu);

// ===== 상태 변경 =====
async function updateStatus(id, next){
  if (next === 'PENDING'){ alert('PENDING 으로는 변경 불가'); return; }
  const ep = {
    APPROVED:  `/api/matching/${id}/approve`,
    REJECTED:  `/api/matching/${id}/reject`,
    COMPLETED: `/api/matching/${id}/complete`,
    CANCELED:  `/api/matching/${id}/cancel`,
  }[next];
  if (!ep) return alert('알 수 없는 상태');
  try{
    const r = await fetch(ep, { method:'POST', headers:{'Accept':'application/json'} });
    if (!r.ok) throw new Error(await r.text());
    await loadData();
  }catch(err){
    alert('상태 변경 실패: ' + err.message);
  }
}

// ===== 데이터 로드 =====
async function loadData(){
  try{
    const id = Number(document.body.dataset.expertId || 0);
    if (!id) throw new Error('expertId 바인딩 안됨.');
    const res = await fetch(`/mypage/api/expert/${id}/matchings`, { headers:{'Accept':'application/json'} });
    if (!res.ok) throw new Error(`API ${res.status}`);
    const data = await res.json();
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

// ===== 수정 버튼 (common.js 모달 우선, 없으면 토글 폴백) =====
document.getElementById('editToggleBtn')?.addEventListener('click', (e)=>{
  if (typeof window.openProfileEditModal === 'function') {
    window.openProfileEditModal();
    return;
  }
  const form = document.getElementById('infoForm');
  if (!form) return;
  const editing = form.dataset.editing === '1';
  form.querySelectorAll('input, select, textarea').forEach(el => {
    if (!el) return;
    if (el.name === 'email') return;
    el.disabled = editing;
  });
  form.dataset.editing = editing ? '0' : '1';
  e.currentTarget.textContent = editing ? '수정' : '저장';
});

// 상단 버튼들
document.getElementById('logoutBtn')?.addEventListener('click', ()=> location.href='/logout');
document.getElementById('reqBtn')?.addEventListener('click', ()=> alert('상담 신청 확인으로 이동'));
document.getElementById('scheduleBtn')?.addEventListener('click', ()=> alert('상담 일정 확인하기로 이동'));
document.getElementById('withdrawBtn')?.addEventListener('click', ()=>{
  if (confirm('정말 탈퇴하시겠습니까?')) alert('탈퇴 처리');
});
