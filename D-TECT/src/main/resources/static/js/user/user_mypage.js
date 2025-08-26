import { initProfileEditPopup, setupLogout, setupWithdraw } from '/js/public/common.js';

(function () {
  // ---------- 신청현황/페이징 ----------
  const userId   = Number(document.body?.dataset?.userId || 0);
  const myMemIdx = Number(document.body?.dataset?.memIdx || 0);
  const listEl   = document.getElementById('applyList');
  const pageInfo = document.getElementById('pageInfo');
  const prevBtn  = document.getElementById('prevPage');
  const nextBtn  = document.getElementById('nextPage');

  const PAGE_SIZE = 5;
  let items = [];
  let page  = 1;

  // 내 정보 수정 (공통)
  initProfileEditPopup('#editToggleBtn');

  // ===== 사유 코드 → 한글 라벨 =====
  const REASON_LABELS = {
    VIOLENCE:"폭력", DEFAMATION:"명예훼손", STALKING:"스토킹", SEXUAL:"성범죄",
    LEAK:"정보유출", BULLYING:"따돌림·집단괴롭힘", CHANTAGE:"협박·갈취", EXTORTION:"공갈·갈취",
  };
  const mapReason = (val) => {
    if (!val) return "—";
    const key = String(val).trim().toUpperCase();
    return REASON_LABELS[key] || val;
  };

  // ===== 유틸 =====
  function fmt(ts){
    if (!ts) return '—';
    const d = new Date(ts); if (Number.isNaN(d.getTime())) return '—';
    const yyyy=d.getFullYear(), mm=String(d.getMonth()+1).padStart(2,'0'), dd=String(d.getDate()).padStart(2,'0');
    const HH=String(d.getHours()).padStart(2,'0'), MM=String(d.getMinutes()).padStart(2,'0');
    return `${yyyy}-${mm}-${dd} ${HH}:${MM}`;
  }

  function statusKorean(s){
    switch(String(s||'').toUpperCase()){
      case 'APPROVED':  return '승인';
      case 'PENDING':   return '대기';
      case 'REJECTED':  return '반려';
      case 'COMPLETED': return '완료';
      case 'CANCELED':  return '취소';
      case 'PAID':      return '결제완료';
      default:          return '대기';
    }
  }

  const badgeClassKor = k =>
      (k==='승인'||k==='완료') ? 'badge badge--ok'
          : (k==='반려'||k==='취소') ? 'badge badge--danger'
              : 'badge badge--warn';

  function rowTemplate(item){
    const requestedAt = item.requestedAt ? fmt(item.requestedAt) : '—';
    const matchedAt   = item.matchedAt  ? fmt(item.matchedAt)  : '—';
    const lawyerName  = (item.lawyerName||'').trim() || '—';

    const reasonRaw = item.reasonLabel || item.requestReason || '';
    const reason    = mapReason(reasonRaw);

    const statusEnum  = String(item.status||'').toUpperCase();
    const sKor        = statusKorean(statusEnum);
    const chatEnabled = (statusEnum==='APPROVED' || statusEnum==='COMPLETED') && !!item.matchingIdx;

    const baseChatUrl = item.chatUrl || (item.matchingIdx ? `/chat/room/${item.matchingIdx}` : '#');
    const chatUrl     = item.matchingIdx
        ? `${baseChatUrl}${baseChatUrl.includes('?') ? '&' : '?'}me=user&mem=${myMemIdx}`
        : '#';

    return `
      <li class="list-row" role="row">
        <div class="col">${requestedAt}</div>
        <div class="col">${lawyerName}</div>
        <div class="col">${reason}</div>
        <div class="col">${matchedAt}</div>
        <div class="col"><span class="${badgeClassKor(sKor)}">${sKor}</span></div>
        <div class="col">
          <button class="chat-btn" data-url="${chatUrl}" ${chatEnabled? '' : 'disabled'}>입장하기</button>
        </div>
      </li>
    `;
  }

  // 채팅 입장 버튼
  listEl?.addEventListener('click', (e) => {
    const btn = e.target.closest('.chat-btn');
    if (!btn || btn.disabled) return;
    const url = btn.dataset.url || '#';
    if (url !== '#') window.location.href = url;
  });

  // ===== 렌더 & 페이지네이션 =====
  function render(){
    if(!listEl||!pageInfo) return;
    const total=items.length, totalPages=Math.max(1, Math.ceil(total/PAGE_SIZE));
    page=Math.min(Math.max(1,page), totalPages);
    pageInfo.textContent=`${page} / ${totalPages}`;
    
    const start=(page-1)*PAGE_SIZE, slice=items.slice(start,start+PAGE_SIZE);

    if(slice.length===0){
      listEl.innerHTML=`<li class="list-row" role="row">
        <div class="col" style="grid-column:1/-1;color:#999">내역이 없습니다.</div>
      </li>`;
      if (prevBtn) prevBtn.disabled = true;
      if (nextBtn) nextBtn.disabled = true;
      return;
    }

    listEl.innerHTML = slice.map(rowTemplate).join('');
    if (prevBtn) prevBtn.disabled = page <= 1;
    if (nextBtn) nextBtn.disabled = page >= totalPages;
  }

  prevBtn?.addEventListener('click', ()=>{ if(page>1){ page--; render(); }});
  nextBtn?.addEventListener('click', ()=>{ page++; render(); });

  // ===== 데이터 로드 =====
  async function load(){
    if(!userId){
      console.warn('userId is missing in data-user-id.');
      items=[]; return render();
    }
    try{
      const res = await fetch(`/mypage/api/user/${userId}/matchings`, {
        headers:{'Accept':'application/json'}
      });
      if(!res.ok) throw new Error(`HTTP ${res.status}`);
      items = await res.json(); // UserMatchingSummaryDto[]
    }catch(e){
      console.error('신청현황 로드 실패:', e);
      items=[];
    }
    page=1; render();
  }

  // ===== 상단/하단 버튼 & 공통 로그아웃 =====
  setupLogout('#logoutBtn', { redirect: '/' });
  setupLogout(document.querySelectorAll('.logout-link'), { allowGetFallback: true });

  document.getElementById('reserveBtn')?.addEventListener('click', (e) => {
    e.preventDefault();
    if (!userId) {
      alert('로그인이 필요합니다.');
      return;
    }
    window.location.href = `/matching/select?userId=${encodeURIComponent(userId)}`;
  });

  // 회원 탈퇴
  setupWithdraw('#withdrawBtn', {
	askEraseData: true,     // 전체 데이터 삭제 여부 묻기
	eraseDefault: true,     // 기본값: 삭제
	// askReason: true,      // 사유 받으려면 주석 해제
});

  document.getElementById('historyBtn')?.addEventListener('click', (e) => {
    e.preventDefault();
    if (!userId) return;
    window.location.href = `/analysis/user/${userId}/history`;
  });

  // 실행
  load();
})();
