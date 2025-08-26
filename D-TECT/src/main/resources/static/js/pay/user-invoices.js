(() => {
    const CLIENT_KEY = "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
    const SUCCESS_URL = "/pay/success";
    const FAIL_URL    = "/pay/fail";

    const $ = (sel, root = document) => root.querySelector(sel);

    const listEl   = $('#invoiceList');
    const emptyEl  = $('#invoiceEmpty');
    const reloadEl = $('#reloadInvoices');

    const modalEl    = $('#payModal');
    const pmTitleEl  = $('#pmInvoiceTitle');
    const pmAmountEl = $('#pmAmount');
    const pmIssuedEl = $('#pmIssuedAt');
    const tpMethodsEl = $('#tp-methods');
    const tpAgreeEl   = $('#tp-agree');

    const userIdStr = (document.body.dataset.userId ?? '').toString().trim();
    const memIdxStr = (document.body.dataset.memIdx ?? '').toString().trim();

    // Toss 규칙(2~50자, A-Za-z0-9-_=.@)을 항상 만족하도록 생성
    function buildCustomerKey() {
        // 우선순위: memIdx → userId → guest_타임스탬프
        let key =
            (memIdxStr && `m_${memIdxStr}`) ||
            (userIdStr && `u_${userIdStr}`) ||
            `guest_${Date.now()}`;

        // 허용문자만 남기기
        key = key.replace(/[^A-Za-z0-9\-_=\.@]/g, '');

        // 길이 보정
        if (key.length < 2) key = `u_${(userIdStr || '00')}`;
        if (key.length > 50) key = key.slice(0, 50);

        return key;
    }

    const customerKey = buildCustomerKey();

    let currentInvoice = null; // { id, title, amount, issuedAt }
    let currentOrderId = null;
    let currentAmount  = null;
    let widgets = null;

    const fmtMoney = (n) => (n||0).toLocaleString('ko-KR') + '원';
    const fmtDate  = (s) => { if(!s) return '—'; const d=new Date(s); return Number.isNaN(d.getTime())? s : d.toLocaleString('ko-KR',{hour12:false}); };
    const escapeHtml = (s) => (s||"").replace(/[&<>"']/g, m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));

    function cardTpl(inv){
        const issued = inv.issuedAt || inv.createdAt;
        return `
      <li class="inv-card"
          data-id="${inv.invoiceId}"
          data-amount="${inv.amountTotal}"
          data-title="${escapeHtml(inv.title||'상담료 청구서')}"
          data-issued="${issued||''}">
        <div>
          <div class="inv-title">${escapeHtml(inv.title||'상담료 청구서')}</div>
          <div class="inv-meta">
            <span>청구금액 <b class="inv-amount">${fmtMoney(inv.amountTotal)}</b></span>
            <span>발행일 ${fmtDate(issued)}</span>
          </div>
        </div>
        <div class="inv-actions">
          <button class="btn btn--primary" data-act="pay">결제</button>
        </div>
      </li>`;
    }

    async function loadInvoices() {
        const userId = Number(userIdStr || 0);
        if (!userId) {
            emptyEl.hidden = false;
            emptyEl.textContent = "로그인이 필요합니다.";
            return;
        }

        // 백엔드 컨트롤러 경로
        const url = `/api/invoices/users/${userId}?status=SENT`;

        listEl.innerHTML = "";
        emptyEl.hidden = true;
        try {
            const res = await fetch(url, { headers:{Accept:'application/json'} });
            if (!res.ok) throw new Error("HTTP "+res.status);
            const rows = await res.json();

            if (!rows || !rows.length) {
                emptyEl.hidden = false;
                emptyEl.textContent = "결제 대기 중인 청구서가 없습니다.";
                return;
            }
            listEl.innerHTML = rows.map(cardTpl).join('');
        } catch (e) {
            console.error('[invoices] load error:', e);
            emptyEl.hidden = false;
            emptyEl.textContent = "청구서를 불러오지 못했습니다.";
        }
    }

    async function openPayModal(invId, title, amount, issuedAt){
        currentInvoice = { id: invId, title, amount, issuedAt };

        // 백엔드 컨트롤러 경로
        const readyUrl = `/api/payments/invoices/${invId}/ready`;

        // 1) READY
        let ready;
        try{
            const res = await fetch(readyUrl, {
                method:'POST',
                headers:{Accept:'application/json'}
            });
            if(!res.ok) throw new Error(await res.text());
            ready = await res.json();
        }catch(e){
            alert("결제 준비에 실패했습니다.");
            console.error(e);
            return;
        }
        currentOrderId = ready.orderId;
        currentAmount  = ready.amount;

        // 2) 모달 정보
        pmTitleEl.textContent  = title || "상담료 청구서";
        pmAmountEl.textContent = fmtMoney(currentAmount);
        pmIssuedEl.textContent = fmtDate(issuedAt) || fmtDate(new Date().toISOString());

        // 3) Toss 위젯
        tpMethodsEl.innerHTML = ""; tpAgreeEl.innerHTML = "";
        const toss = window.TossPayments ? TossPayments(CLIENT_KEY) : null;
        if (!toss) {
            alert('결제 위젯을 불러오지 못했습니다. (Toss SDK)');
            return;
        }
        widgets = toss.widgets({ customerKey });

        await widgets.setAmount({ currency:'KRW', value: currentAmount });
        await widgets.renderPaymentMethods({ selector:'#tp-methods', variantKey:'DEFAULT' });
        await widgets.renderAgreement({ selector:'#tp-agree', variantKey:'AGREEMENT' });

        modalEl.hidden = false;
    }

    function closePayModal(){
        modalEl.hidden = true;
        currentInvoice = null;
        currentOrderId = null;
        currentAmount  = null;
        widgets = null;
        tpMethodsEl.innerHTML = "";
        tpAgreeEl.innerHTML = "";
    }

    // 리스트 클릭 위임
    listEl?.addEventListener('click', (e)=>{
        const btn = e.target.closest('button[data-act="pay"]');
        if(!btn) return;
        const li = btn.closest('.inv-card');
        const id     = Number(li.dataset.id);
        const title  = li.dataset.title;
        const amount = Number(li.dataset.amount);
        const issued = li.dataset.issued || '';
        openPayModal(id, title, amount, issued);
    });

    reloadEl?.addEventListener('click', loadInvoices);
    $('#pmClose')?.addEventListener('click', closePayModal);
    $('#pmCancelBtn')?.addEventListener('click', closePayModal);
    modalEl?.addEventListener('click', (e)=>{ if(e.target === modalEl) closePayModal(); });

    // 결제하기
    $('#pmPayBtn')?.addEventListener('click', async ()=>{
        if(!widgets || !currentInvoice || !currentOrderId) return;
        try{
            await widgets.setAmount({ currency:'KRW', value: currentAmount });
            await widgets.requestPayment({
                orderId: currentOrderId,
                orderName: currentInvoice.title || "상담료",
                successUrl: window.location.origin + SUCCESS_URL,
                failUrl: window.location.origin + FAIL_URL,
            });
        }catch(err){
            console.error('[invoices] requestPayment error:', err);
            alert("결제 요청 중 오류가 발생했습니다.");
        }
    });

    document.addEventListener('DOMContentLoaded', loadInvoices);
})();
