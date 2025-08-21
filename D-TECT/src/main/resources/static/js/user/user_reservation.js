// 요소
const monthLabel = document.getElementById('monthLabel');
const grid = document.getElementById('calendarGrid');
const prevBtn = document.getElementById('prevMonth');
const nextBtn = document.getElementById('nextMonth');
const reserveBtn = document.getElementById('reserveBtn');
const cancelBtn = document.getElementById('cancelBtn');

let view = new Date(); view.setDate(1);
let selectedISO = null;

// 이미 막혀있는 날짜(예시)
const booked = new Set([
    // '2025-08-15'
]);

// 내가 예약 완료한 날짜 (취소 가능)
const myBookings = new Set();

// 유틸
const pad = n => String(n).padStart(2, '0');
const toISO = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const startOfMonth = d => { const x = new Date(d); x.setDate(1); return x; };
const endOfMonth = d => { const x = new Date(d); x.setMonth(x.getMonth() + 1); x.setDate(0); return x; };

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
            const isBooked = booked.has(iso);
            const isMine = myBookings.has(iso);

            // 기본 마크업
            let meta = '';
            if (isBooked && isMine) meta = '내 예약';
            else if (isBooked) meta = '예약불가';

            cell.innerHTML = `<div class="${numCls}">${n}</div><div class="meta">${meta}</div>`;

            if (isPast) {
                cell.classList.add('day--past', 'day--disabled');
            } else {
                if (isBooked) {
                    cell.classList.add('day--booked');
                    if (isMine) {
                        // ✅ 내 예약은 선택 가능 (취소를 위해)
                        cell.classList.add('day--mine');
                        cell.addEventListener('click', () => selectDate(iso, cell));
                    } else {
                        // 남의 예약은 클릭 불가
                        cell.classList.add('day--disabled');
                    }
                } else {
                    // 예약 가능 날짜
                    cell.addEventListener('click', () => selectDate(iso, cell));
                }
            }

            // 다시 그릴 때 선택 유지
            if (selectedISO === iso) {
                cell.classList.add('day--selected');
            }
        }

        grid.appendChild(cell);
    }
}

function selectDate(iso, cell) {
    [...grid.querySelectorAll('.day--selected')].forEach(n => n.classList.remove('day--selected'));
    selectedISO = iso;
    cell.classList.add('day--selected');
}

// 이벤트
prevBtn.addEventListener('click', () => { view.setMonth(view.getMonth() - 1); render(); });
nextBtn.addEventListener('click', () => { view.setMonth(view.getMonth() + 1); render(); });

reserveBtn.addEventListener('click', () => {
    if (!selectedISO) { alert('예약할 날짜를 선택해주세요.'); return; }

    if (booked.has(selectedISO) && !myBookings.has(selectedISO)) {
        alert('이미 예약 불가한 날짜입니다.');
        return;
    }
    if (myBookings.has(selectedISO)) {
        alert('이미 예약하신 날짜입니다.');
        return;
    }

    // TODO: 예약 API 호출 (selectedISO 전송)
    const ok = true;
    if (ok) {
        booked.add(selectedISO);
        myBookings.add(selectedISO);
        alert('예약이 완료되었습니다.');
        render();
    } else {
        alert('작업이 완료되지 않았습니다.');
    }
});

cancelBtn.addEventListener('click', () => {
    if (!selectedISO) { alert('취소할 날짜를 선택해주세요.'); return; }
    if (!myBookings.has(selectedISO)) { alert('내가 예약한 날짜만 취소할 수 있습니다.'); return; }

    // TODO: 예약 취소 API 호출
    const ok = true;
    if (ok) {
        booked.delete(selectedISO);
        myBookings.delete(selectedISO);
        alert('예약 취소가 완료되었습니다.');
        // 선택 해제 후 다시 그림
        selectedISO = null;
        render();
    } else {
        alert('작업이 완료되지 않았습니다.');
    }
});

// 최초 렌더
render();
