/* ===== 더미 데이터 (API 연동 시 이 배열을 서버 응답으로 교체) ===== */
const APPLY_DATA = [
    { id: 1, applyDate: '2025-08-14', lawyer: '김○○', type: '교복', matchDate: '—', matchStat: '접수완료' },
    { id: 2, applyDate: '2025-08-14', lawyer: '박○○', type: '상담예약', matchDate: '2025-08-16', matchStat: '확정' },
    { id: 3, applyDate: '2025-08-15', lawyer: '—', type: '정책지원', matchDate: '—', matchStat: '반려' },
    { id: 4, applyDate: '2025-08-16', lawyer: '이○○', type: '상담예약', matchDate: '—', matchStat: '대기' },
    { id: 5, applyDate: '2025-08-17', lawyer: '최○○', type: '정책지원', matchDate: '2025-08-20', matchStat: '확정' },
    { id: 6, applyDate: '2025-08-18', lawyer: '김○○', type: '교복', matchDate: '—', matchStat: '접수완료' },
];

const PAGE_SIZE = 5;
let page = 1;

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
      <div class="col">${item.lawyer}</div>
      <div class="col">${item.type}</div>
      <div class="col">${item.matchDate}</div>
      <div class="col"><span class="${badgeClass(item.matchStat)}">${item.matchStat}</span></div>
    </li>
  `).join('');
}

prevBtn.addEventListener('click', () => { page--; renderList(); });
nextBtn.addEventListener('click', () => { page++; renderList(); });

/* ===== 개인정보 편집 토글 ===== */
const editToggleBtn = document.getElementById('editToggleBtn');
const infoForm = document.getElementById('infoForm');
const saveBtn = document.getElementById('saveBtn');
const cancelBtn = document.getElementById('cancelBtn');

function setEditMode(edit) {
    [...infoForm.querySelectorAll('input')].forEach(i => i.disabled = !edit);
    saveBtn.disabled = !edit;
    cancelBtn.disabled = !edit;
    editToggleBtn.textContent = edit ? '편집중' : '수정';
}
let backup = {};
editToggleBtn.addEventListener('click', () => {
    const startingEdit = saveBtn.disabled; // 비활성화면 편집 시작
    if (startingEdit) {
        backup = {};
        infoForm.querySelectorAll('input').forEach(i => backup[i.name] = i.value);
    }
    setEditMode(startingEdit);
});
cancelBtn.addEventListener('click', () => {
    infoForm.querySelectorAll('input').forEach(i => i.value = backup[i.name] ?? i.value);
    setEditMode(false);
});
infoForm.addEventListener('submit', (e) => {
    e.preventDefault();
    // TODO: 저장 API 연동
    alert('저장되었습니다.');
    setEditMode(false);
});

/* ===== 상단/하단 버튼 샘플 ===== */
document.getElementById('logoutBtn').addEventListener('click', () => alert('로그아웃 처리'));
document.getElementById('historyBtn').addEventListener('click', () => alert('내 분석 내역으로 이동'));
document.getElementById('reserveBtn').addEventListener('click', () => alert('상담 일정 예약하기로 이동'));
document.getElementById('withdrawBtn').addEventListener('click', () => {
    if (confirm('정말로 회원을 탈퇴하시겠습니까?')) alert('탈퇴 처리');
});

/* 초기 렌더 */
renderList();
