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

// 공통 팝업 호출 (전문분야 다중 선택 기본)
// 서버에서 내려준 옵션을 전역에 실어두었다면:
initProfileEditPopup('#editToggleBtn', {
  specialtyOptions: window.__SPECIALTIES__ || [], // [{code,label}] 서버에서 내려준 옵션
  // specialtyInputType: 'radio'  // 라디오가 필요할 때만 지정, 기본은 체크박스
});
// 내 memIdx 읽기
const myMemIdx = Number(document.body.dataset.memIdx || 0);

// ===== 유틸 =====
function fmt(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toISOString().slice(0, 10);
}
function statusKorean(status) {
  switch (String(status || '').toUpperCase()) {
    case 'APPROVED':  return '확정';
    case 'REJECTED':  return '반려';
    case 'PENDING':   return '대기';
    case 'COMPLETED': return '완료';
    case 'CANCELED':  return '취소';
    default:          return '—';
  }
}
function badgeClassKor(k) {
  if (k==='확정'||k==='완료') return 'badge badge--ok';
  if (k==='반려'||k==='취소') return 'badge badge--danger';
  return 'badge badge--warn';
}
function chatEnabledByStatus(statusEnumUpper) {
  return statusEnumUpper === 'APPROVED' || statusEnumUpper === 'COMPLETED';
}
function rowTemplate(item) {
  const requestedAt = fmt(item.requestedAt);
  const matchedAt   = fmt(item.matchedAt);
  const customer    = item.customerName ?? '—';
  const reason      = item.requestReason ?? '—';
  const statusEnum  = String(item.status || '').toUpperCase();
  const sKor        = statusKorean(statusEnum);
  const enabled     = chatEnabledByStatus(statusEnum);
  // ✔ 채팅방 링크에 me=expert & mem=<내memIdx> 부여
  const chatUrl     = item.chatUrl || `/chat/room/${item.matchingIdx}?me=expert&mem=${myMemIdx}`;

    return `
    <li class="list-row" role="row">
      <div class="col">${requestedAt}</div>
      <div class="col">${customer}</div>
      <div class="col">${reason}</div>
      <div class="col">${matchedAt}</div>
      <div class="col"><span class="${badgeClassKor(sKor)}">${sKor}</span></div>
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

// 목록 렌더 & 페이지네이션
function renderList() {
  const total = allItems.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  page = Math.min(Math.max(1, page), totalPages);

  pageInfo.textContent = `${page} / ${totalPages}`;

  const start = (page - 1) * PAGE_SIZE;
  const items = allItems.slice(start, start + PAGE_SIZE);

  if (items.length === 0) {
    listEl.innerHTML = `<li class="list-row">
      <div class="col" style="grid-column:1/7;text-align:center;color:#888">데이터가 없습니다.</div>
    </li>`;
  } else {
    listEl.innerHTML = items.map(rowTemplate).join('');
  }
  hydrateCaseSelect(items);
  prevBtn && (prevBtn.disabled = page <= 1);
  nextBtn && (nextBtn.disabled = page >= totalPages);
}
prevBtn?.addEventListener('click', () => { page--; renderList(); });
nextBtn?.addEventListener('click', () => { page++; renderList(); });

// 상담료 케이스 셀렉트
function hydrateCaseSelect(visibleItems) {
  const options = visibleItems.map(i =>
    `<option value="${i.matchingIdx}">[${fmt(i.requestedAt)}] ${i.customerName ?? '—'} - ${i.requestReason ?? '—'}</option>`
  );
  caseSelect.innerHTML = options.join('');
}
feeForm?.addEventListener('submit', (e) => {
  e.preventDefault();
  const id   = caseSelect.value;
  const memo = (memoInput.value || '').trim();
  const fee  = Number(feeInput.value || 0);

  if (!id) { alert('케이스를 선택하세요.'); return; }
  if (!(fee > 0)) { alert('상담료를 입력하세요.'); return; }
  // TODO: 서버 전송
  console.log('상담료 저장', { caseId: id, fee, memo });
  alert('상담료가 저장되었습니다.');
  memoInput.value = '';
  feeInput.value  = '';
});

// 데이터 로드
async function loadData() {
  try {
    const id = Number(document.body.dataset.expertId || 0);
    if (!id) throw new Error('expertId가 바인딩되지 않았습니다.');

    const res = await fetch(`/mypage/api/expert/${id}/matchings`, { headers: { 'Accept':'application/json' } });
    if (!res.ok) throw new Error(`API Error: ${res.status}`);
    const data = await res.json(); // ExpertMatchingSummaryDto[]
    allItems = Array.isArray(data)
      ? data.sort((a, b) => new Date(b.requestedAt) - new Date(a.requestedAt))
      : [];
  } catch (e) {
    console.error(e);
    allItems = [];
  } finally {
    renderList();
  }
}
loadData();

// 상단 버튼
document.getElementById('logoutBtn')?.addEventListener('click', () => { window.location.href = '/logout'; });
document.getElementById('reqBtn')?.addEventListener('click', () => alert('상담 신청 확인으로 이동'));
document.getElementById('scheduleBtn')?.addEventListener('click', () => alert('상담 일정 확인하기로 이동'));
document.getElementById('withdrawBtn')?.addEventListener('click', () => {
  if (confirm('정말로 회원을 탈퇴하시겠습니까?')) alert('탈퇴 처리');
});
