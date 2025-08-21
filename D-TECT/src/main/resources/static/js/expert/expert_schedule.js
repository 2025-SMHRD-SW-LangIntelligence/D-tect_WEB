// 요소
const monthLabel = document.getElementById('monthLabel');
const grid = document.getElementById('calendarGrid');
const prevBtn = document.getElementById('prevMonth');
const nextBtn = document.getElementById('nextMonth');
const approveBtn = document.getElementById('approveBtn');
const cancelBtn = document.getElementById('cancelBtn');

let view = new Date(); view.setDate(1);
let selectedISO = null;

/* 상태 저장
   - pending: 사용자가 신청 → 전문가 승인 대기(노란색)
   - approved: 전문가 승인 완료(예약됨, 빨간 텍스트)
   * 실제 환경에선 서버에서 받아와 채워주세요. */
const pending = new Set(['2025-08-06', '2025-08-15']);
const approved = new Set(['2025-08-20']);

const pad = n => String(n).padStart(2, '0');
const toISO = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

function startOfMonth(d) { const x = new Date(d); x.setDate(1); return x; }
function endOfMonth(d) { const x = new Date(d); x.setMonth(x.getMonth() + 1); x.setDate(0); return x; }

function render() {
    const y = view.getFullYear();
    const m = view.getMonth();
    monthLabel.textContent = `${y}-${pad(m + 1)}`;

    grid.innerHTML = '';
    const first = startOfMonth(view);
    const last = endOfMonth(view);
    const startIdx = (first.getDay() + 7) % 7;
    const totalDays = last.getDate();
    const prevLast = new Date(y, m, 0).getDate();
    const today = new Date(); today.setHours(0, 0, 0, 0);

    for (let i = 0; i < 42; i++) {
        const cell = document.createElement('div');
        cell.className = 'day';

        let date;
        if (i < startIdx) {
            const n = prevLast - (startIdx - 1 - i);
            date = new Date(y, m, - (startIdx - i) + 1);
            cell.classList.add('day--other', 'day--disabled');
            cell.innerHTML = `<div class="num">${n}</div>`;
        } else if (i >= startIdx + totalDays) {
            const n = i - (startIdx + totalDays) + 1;
            date = new Date(y, m + 1, n);
            cell.classList.add('day--other', 'day--disabled');
            cell.innerHTML = `<div class="num">${n}</div>`;
        } else {
            const n = i - startIdx + 1;
            date = new Date(y, m, n);

            const dow = i % 7;
            const numCls = dow === 0 ? 'num sun' : dow === 6 ? 'num sat' : 'num';
            const iso = toISO(date);
            const isPast = date < today;
            const isPending = pending.has(iso);
            const isApproved = approved.has(iso);

            let meta = '';
            if (isApproved) meta = '예약됨';
            else if (isPending) meta = '승인 대기';

            cell.innerHTML = `<div class="${numCls}">${n}</div><div class="meta">${meta}</div>`;

            if (isPast) {
                cell.classList.add('day--past', 'day--disabled');
            } else {
                if (isApproved) cell.classList.add('day--approved');
                if (isPending) cell.classList.add('day--pending');
                // 전문가 페이지에서는 pending/approved 모두 선택 가능 (승인/취소를 위해)
                cell.addEventListener('click', () => selectDate(iso, cell));
            }

            if (selectedISO === iso) cell.classList.add('day--selected');
        }

        grid.appendChild(cell);
    }
}

function selectDate(iso, cell) {
    [...grid.querySelectorAll('.day--selected')].forEach(n => n.classList.remove('day--selected'));
    selectedISO = iso;
    cell.classList.add('day--selected');
}

/* 버튼 동작 */
approveBtn.addEventListener('click', () => {
    if (!selectedISO) { alert('승인할 날짜를 선택해주세요.'); return; }
    if (!pending.has(selectedISO)) {
        alert('승인 대기 중인 예약이 없습니다.');
        return;
    }
    // TODO: 승인 API 호출
    const ok = true;
    if (ok) {
        pending.delete(selectedISO);
        approved.add(selectedISO);
        alert('작업이 완료되었습니다.');   // 승인 성공
        render();
    } else {
        alert('작업이 완료되지 않았습니다.');
    }
});

cancelBtn.addEventListener('click', () => {
    if (!selectedISO) { alert('취소할 날짜를 선택해주세요.'); return; }
    if (!pending.has(selectedISO) && !approved.has(selectedISO)) {
        alert('취소할 예약이 없습니다.');
        return;
    }
    // TODO: 취소 API 호출
    const ok = true;
    if (ok) {
        pending.delete(selectedISO);
        approved.delete(selectedISO);
        alert('작업이 완료되었습니다.');   // 취소 성공
        selectedISO = null;
        render();
    } else {
        alert('작업이 완료되지 않았습니다.');
    }
});

/* 월 전환 */
document.getElementById('prevMonth').addEventListener('click', () => { view.setMonth(view.getMonth() - 1); render(); });
document.getElementById('nextMonth').addEventListener('click', () => { view.setMonth(view.getMonth() + 1); render(); });

/* 초기 렌더 */
render();

/* ===== API 연동 가이드 =====
- 서버에서 해당 월의 예약 목록을 받아 pending/approved를 채우세요.
  예) GET /api/expert/schedules?month=2025-08
  응답 예시: { pending: ['2025-08-06'], approved: ['2025-08-20'] }

- 승인/취소 시 위의 TODO 위치에서 fetch 호출 후 ok 여부에 따라 상태를 갱신하세요.
*/
