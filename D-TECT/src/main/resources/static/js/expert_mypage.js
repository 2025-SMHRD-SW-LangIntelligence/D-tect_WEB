/* ===== 신청현황 더미 데이터 =====
   ↳ 실제 API 연동 시 이 배열을 서버 응답으로 대체 */
const APPLY_DATA = [
    { id: 1, applyDate: '2025-08-14', client: '김○○', type: '상담예약', matchDate: '2025-08-16', matchStat: '확정' },
    { id: 2, applyDate: '2025-08-14', client: '박○○', type: '정책지원', matchDate: '—', matchStat: '접수완료' },
    { id: 3, applyDate: '2025-08-15', client: '이○○', type: '교복', matchDate: '—', matchStat: '대기' },
    { id: 4, applyDate: '2025-08-16', client: '최○○', type: '상담예약', matchDate: '—', matchStat: '반려' },
    { id: 5, applyDate: '2025-08-17', client: '정○○', type: '정책지원', matchDate: '2025-08-20', matchStat: '확정' },
    { id: 6, applyDate: '2025-08-18', client: '오○○', type: '상담예약', matchDate: '—', matchStat: '접수완료' },
];

const PAGE_SIZE = 5;
let page = 1;

/* DOM */
const listEl = document.getElementById('applyList');
const pageInfo = document.getElementById('pageInfo');
const prevBtn = document.getElementById('prevPage');
const nextBtn = document.getElementById('nextPage');

function badgeClass(stat) {
    const s = String(stat).trim();
    if (s === '확정') return 'badge badge--ok';
    if (s === '반려') return 'badge badge--danger';
    return 'badge badge--warn'; // 접수완료, 대기 등
}

function renderList() {
    const total = APPLY_DATA.length;
    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    page = Math.min(Math.max(1, page), totalPages);
    pageInfo.textContent = `${page} / ${totalPages}`;

    const start = (page - 1) * PAGE_SIZE;
    const items = APPLY_DATA.slice(start, start + PAGE_SIZE);

    listEl.innerHTML = items.map(item => `
    <li class="list-row" role="row">
      <div class="col">${item.applyDate}</div>
      <div class="col">${item.client}</div>
      <div class="col">${item.type}</div>
      <div class="col">${item.matchDate}</div>
      <div class="col"><span class="${badgeClass(item.matchStat)}">${item.matchStat}</span></div>
    </li>
  `).join('');

    /* 상담료 케이스 선택 박스 갱신 */
    hydrateCaseSelect(items);
}

prevBtn.addEventListener('click', () => { page--; renderList(); });
nextBtn.addEventListener('click', () => { page++; renderList(); });

/* ===== 개인정보 편집 토글 ===== */
const editToggleBtn = document.getElementById('editToggleBtn');
const infoForm = document.getElementById('infoForm');
const saveBtn = document.getElementById('saveBtn');
const cancelBtn = document.getElementById('cancelBtn');
const specialties = document.getElementById('specialties');

function setEditMode(edit) {
    [...infoForm.querySelectorAll('input'), specialties].forEach(i => i.disabled = !edit);
    saveBtn.disabled = !edit;
    cancelBtn.disabled = !edit;
    editToggleBtn.textContent = edit ? '편집중' : '수정';
}
let backup = {};
editToggleBtn.addEventListener('click', () => {
    const startingEdit = saveBtn.disabled; // 비활성=편집시작
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
cancelBtn.addEventListener('click', () => {
    infoForm.querySelectorAll('input, select').forEach(el => {
        if (el.tagName === 'SELECT' && el.multiple) {
            [...el.options].forEach(o => o.selected = (backup[el.name] || []).includes(o.value));
        } else {
            el.value = backup[el.name] ?? el.value;
        }
    });
    setEditMode(false);
});
infoForm.addEventListener('submit', (e) => {
    e.preventDefault();
    // TODO: 실제 저장 API 연동
    alert('개인정보가 저장되었습니다.');
    setEditMode(false);
});

/* ===== 상담료 작성 ===== */
const caseSelect = document.getElementById('caseSelect');
const feeForm = document.getElementById('feeForm');
const feeInput = document.getElementById('fee');
const memoInput = document.getElementById('memo');

function hydrateCaseSelect(visibleItems) {
    // 현재 페이지의 항목으로 옵션 구성 (실서비스에선 전체/검색 등으로 변경)
    const options = visibleItems.map(i => `<option value="${i.id}">[${i.applyDate}] ${i.client} - ${i.type}</option>`);
    caseSelect.innerHTML = options.join('');
}

feeForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const id = caseSelect.value;
    const memo = (memoInput.value || '').trim();
    const fee = Number(feeInput.value || 0);

    if (!id) { alert('케이스를 선택하세요.'); return; }
    if (!(fee > 0)) { alert('상담료를 입력하세요.'); return; }

    // TODO: 서버 전송
    console.log('상담료 저장', { caseId: id, fee, memo });
    alert('상담료가 저장되었습니다.');
    memoInput.value = '';
    feeInput.value = '';
});

/* ===== 상단/하단 버튼 샘플 동작 ===== */
document.getElementById('logoutBtn').addEventListener('click', () => alert('로그아웃 처리'));
document.getElementById('reqBtn').addEventListener('click', () => alert('상담 신청 확인으로 이동'));
document.getElementById('scheduleBtn').addEventListener('click', () => alert('상담 일정 확인하기로 이동'));
document.getElementById('withdrawBtn').addEventListener('click', () => {
    if (confirm('정말로 회원을 탈퇴하시겠습니까?')) alert('탈퇴 처리');
});

/* 초기 렌더 */
renderList();
