(function () {
    const userId   = Number(document.body.dataset.userId || 0);
    const listEl   = document.getElementById('applyList');
    const pageInfo = document.getElementById('pageInfo');
    const prevBtn  = document.getElementById('prevPage');
    const nextBtn  = document.getElementById('nextPage');

    const PAGE_SIZE = 5;
    let items = [];
    let page  = 1;

    // ===== 유틸 =====
    function fmt(ts) {
        if (!ts) return '—';
        const d = new Date(ts);
        if (Number.isNaN(d.getTime())) return '—';
        const yyyy = d.getFullYear();
        const mm   = String(d.getMonth() + 1).padStart(2, '0');
        const dd   = String(d.getDate()).padStart(2, '0');
        const HH   = String(d.getHours()).padStart(2, '0');
        const MM   = String(d.getMinutes()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd} ${HH}:${MM}`;
    }

    function statusKorean(s) {
        if (!s) return '대기';
        switch (String(s).toUpperCase()) {
            case 'APPROVED':  return '확정';
            case 'REJECTED':  return '반려';
            case 'COMPLETED': return '완료';
            case 'CANCELED':  return '취소';
            case 'PENDING':
            default:          return '대기';
        }
    }

    function badgeClassKor(kor) {
        if (kor === '확정' || kor === '완료') return 'badge badge--ok';
        if (kor === '반려' || kor === '취소') return 'badge badge--danger';
        return 'badge badge--warn';
    }

    // ===== 한 행 템플릿(채팅 버튼 포함) =====
    function rowTemplate(item) {
        const requestedAt = item.requestedAt ? fmt(item.requestedAt) : '—';
        const matchedAt   = item.matchedAt   ? fmt(item.matchedAt)   : '—';
        const lawyerName  = item.lawyerName && item.lawyerName.trim() ? item.lawyerName : '—';
        const reason      = item.requestReason && item.requestReason.trim() ? item.requestReason : '—';

        const statusEnum  = String(item.status || '').toUpperCase(); // APPROVED/REJECTED/...
        const sKor        = statusKorean(statusEnum);
        const chatEnabled = (statusEnum === 'APPROVED' || statusEnum === 'COMPLETED');

        // 서버에서 chatUrl을 내려주지 않는다면 matchingIdx로 기본 경로 생성
        const chatUrl     = item.chatUrl || `/chat/room/${item.matchingIdx}`;

        return `
      <li class="list-row" role="row">
        <div class="col">${requestedAt}</div>
        <div class="col">${lawyerName}</div>
        <div class="col">${reason}</div>
        <div class="col">${matchedAt}</div>
        <div class="col"><span class="${badgeClassKor(sKor)}">${sKor}</span></div>
        <div class="col">
          <button class="chat-btn" data-url="${chatUrl}" ${chatEnabled ? '' : 'disabled'}>입장하기</button>
        </div>
      </li>
    `;
    }

    // 채팅 버튼 클릭 위임
    listEl.addEventListener('click', (e) => {
        const btn = e.target.closest('.chat-btn');
        if (!btn || btn.disabled) return;
        const url = btn.dataset.url || '#';
        window.location.href = url;
    });

    // ===== 렌더 & 페이지네이션 =====
    function render() {
        const total = items.length;
        const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
        page = Math.min(Math.max(1, page), totalPages);

        pageInfo.textContent = `${page} / ${totalPages}`;

        const start = (page - 1) * PAGE_SIZE;
        const slice = items.slice(start, start + PAGE_SIZE);

        if (slice.length === 0) {
            listEl.innerHTML = `
        <li class="list-row" role="row">
          <div class="col" style="grid-column:1/-1;color:#999">내역이 없습니다.</div>
        </li>`;
            prevBtn.disabled = true;
            nextBtn.disabled = true;
            return;
        }

        listEl.innerHTML = slice.map(rowTemplate).join('');
        prevBtn.disabled = page <= 1;
        nextBtn.disabled = page >= totalPages;
    }

    prevBtn?.addEventListener('click', () => { if (page > 1) { page--; render(); }});
    nextBtn?.addEventListener('click', () => { page++; render(); });

    // ===== 데이터 로드 =====
    async function load() {
        if (!userId) {
            console.warn('userId is missing in data-user-id.');
            items = [];
            return render();
        }
        try {
            const res = await fetch(`/mypage/api/user/${userId}/matchings`, {
                headers: { 'Accept': 'application/json' }
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            items = await res.json(); // UserMatchingSummaryDto[]
        } catch (e) {
            console.error('신청현황 로드 실패:', e);
            items = [];
        }
        page = 1;
        render();
    }

    // ===== 개인정보 폼 데모 동작(원래 코드 유지) =====
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
    editToggleBtn?.addEventListener('click', () => {
        const startingEdit = saveBtn.disabled;
        if (startingEdit) {
            backup = {};
            infoForm.querySelectorAll('input').forEach(i => backup[i.name] = i.value);
        }
        setEditMode(startingEdit);
    });
    cancelBtn?.addEventListener('click', () => {
        infoForm.querySelectorAll('input').forEach(i => i.value = backup[i.name] ?? i.value);
        setEditMode(false);
    });
    infoForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        alert('저장되었습니다.'); // TODO: 저장 API 연동
        setEditMode(false);
    });

    // 상/하단 버튼(데모)
    document.getElementById('logoutBtn')?.addEventListener('click', () => alert('로그아웃 처리'));
    document.getElementById('historyBtn')?.addEventListener('click', () => {
        const userId = Number(document.body.dataset.userId || 0);
        if (!userId) {
            alert('로그인이 만료되었거나 사용자 정보가 없습니다.');
            return;
        }
        window.location.href = `/analysis/user/${userId}/history`;
    });
    document.getElementById('reserveBtn')?.addEventListener('click', () => alert('상담 일정 예약하기로 이동'));
    document.getElementById('withdrawBtn')?.addEventListener('click', () => {
        if (confirm('정말로 회원을 탈퇴하시겠습니까?')) alert('탈퇴 처리');
    });

    // 초기 로드
    load();
})();
