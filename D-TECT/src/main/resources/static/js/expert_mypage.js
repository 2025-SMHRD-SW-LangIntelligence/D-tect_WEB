const PAGE_SIZE = 5;

let page = 1;
let allItems = []; // 서버에서 받아온 전체 목록

// DOM
const listEl     = document.getElementById('applyList');
const pageInfo   = document.getElementById('pageInfo');
const prevBtn    = document.getElementById('prevPage');
const nextBtn    = document.getElementById('nextPage');

const editToggleBtn = document.getElementById('editToggleBtn');
const infoForm      = document.getElementById('infoForm');
const saveBtn       = document.getElementById('saveBtn');
const cancelBtn     = document.getElementById('cancelBtn');
const specialties   = document.getElementById('specialties');

const caseSelect = document.getElementById('caseSelect');
const feeForm    = document.getElementById('feeForm');
const feeInput   = document.getElementById('fee');
const memoInput  = document.getElementById('memo');

// ===== 유틸 =====
function fmt(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toISOString().slice(0, 10); // YYYY-MM-DD
}

function statusKorean(status) {
    if (!status) return '—';
    switch (String(status).toUpperCase()) {
        case 'APPROVED':  return '확정';
        case 'REJECTED':  return '반려';
        case 'PENDING':   return '대기';
        case 'COMPLETED': return '완료';
        case 'CANCELED':  return '취소';
        default:          return status;
    }
}

function badgeClassKor(kor) {
    switch (kor) {
        case '확정':
        case '완료': return 'badge badge--ok';
        case '반려':
        case '취소': return 'badge badge--danger';
        case '대기':
        default:     return 'badge badge--warn';
    }
}

// 채팅 활성 조건(전문가도 동일 로직)
function chatEnabledByStatus(statusEnumUpper) {
    return statusEnumUpper === 'APPROVED' || statusEnumUpper === 'COMPLETED';
}

// 한 행 템플릿: 6열(신청일/고객명/분류/매칭일/매칭여부/채팅방)
function rowTemplate(item) {
    const requestedAt = fmt(item.requestedAt);
    const matchedAt   = fmt(item.matchedAt);
    const customer    = item.customerName ?? '—';
    const reason      = item.requestReason ?? '—';

    const statusEnum  = String(item.status || '').toUpperCase();
    const sKor        = statusKorean(statusEnum);

    const enabled     = chatEnabledByStatus(statusEnum);
    const chatUrl     = item.chatUrl || `/chat/room/${item.matchingIdx}`;

    return `
    <li class="list-row" role="row">
      <div class="col">${requestedAt}</div>
      <div class="col">${customer}</div>
      <div class="col">${reason}</div>
      <div class="col">${matchedAt}</div>
      <div class="col"><span class="${badgeClassKor(sKor)}">${sKor}</span></div>
      <div class="col">
        <button class="chat-btn" data-url="${chatUrl}" ${enabled ? '' : 'disabled'}>입장하기</button>
      </div>
    </li>
  `;
}

// 채팅 버튼 델리게이션
listEl.addEventListener('click', (e) => {
    const btn = e.target.closest('.chat-btn');
    if (!btn || btn.disabled) return;
    const url = btn.dataset.url || '#';
    window.location.href = url;
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

    // 우하단 상담료 케이스 선택(현재 페이지 기준)
    hydrateCaseSelect(items);
}

prevBtn?.addEventListener('click', () => { page--; renderList(); });
nextBtn?.addEventListener('click', () => { page++; renderList(); });

// 개인정보 편집(데모)
function setEditMode(edit) {
    [...infoForm.querySelectorAll('input'), specialties].forEach(i => i.disabled = !edit);
    saveBtn.disabled = !edit;
    cancelBtn.disabled = !edit;
    editToggleBtn.textContent = edit ? '편집중' : '수정';
}
let backup = {};
editToggleBtn?.addEventListener('click', () => {
    const startingEdit = saveBtn.disabled;
    if (startingEdit) {
        backup = {};
        infoForm.querySelectorAll('input, select').forEach(el => {
            if (el.tagName === 'SELECT' && el.multiple) {
                backup[el.name] = [...el.options].filter(o => o.selected).map(o => o.value);
            } else {
                backup[el.name] = el.value;
            }
        });
    }
    setEditMode(startingEdit);
});
cancelBtn?.addEventListener('click', () => {
    infoForm.querySelectorAll('input, select').forEach(el => {
        if (el.tagName === 'SELECT' && el.multiple) {
            [...el.options].forEach(o => o.selected = (backup[el.name] || []).includes(o.value));
        } else {
            el.value = backup[el.name] ?? el.value;
        }
    });
    setEditMode(false);
});
infoForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    alert('개인정보가 저장되었습니다.'); // TODO: 실제 저장 API 연동
    setEditMode(false);
});

// 상담료 작성(데모)
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

// 상단 버튼(데모)
document.getElementById('logoutBtn')?.addEventListener('click', () => alert('로그아웃 처리'));
document.getElementById('reqBtn')?.addEventListener('click', () => alert('상담 신청 확인으로 이동'));
document.getElementById('scheduleBtn')?.addEventListener('click', () => alert('상담 일정 확인하기로 이동'));
document.getElementById('withdrawBtn')?.addEventListener('click', () => {
    if (confirm('정말로 회원을 탈퇴하시겠습니까?')) alert('탈퇴 처리');
});

// 데이터 로드
async function loadData() {
    try {
        const expertId = Number(document.body.dataset.expertId || 0);
        if (!expertId) throw new Error('expertId가 바인딩되지 않았습니다.');

        const res = await fetch(`/mypage/api/expert/${expertId}/matchings`, {
            headers: { 'Accept': 'application/json' }
        });
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
