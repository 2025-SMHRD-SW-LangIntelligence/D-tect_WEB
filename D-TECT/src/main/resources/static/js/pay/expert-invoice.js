(() => {
    const $ = (sel, root = document) => root.querySelector(sel);

    const parseAmount = (v) => parseInt(String(v || '').replace(/[^\d]/g, ''), 10) || 0;

    function bindAmountFormatter(inputEl) {
        if (!inputEl || inputEl.dataset.bound) return;
        inputEl.addEventListener('input', (e) => {
            const el = e.target;
            const prev = el.value;
            const caret = el.selectionStart ?? prev.length;

            const digits = prev.replace(/[^\d]/g, '');
            const normalized = digits.replace(/^0+(?=\d)/, '');
            const withComma = normalized.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

            const diff = withComma.length - prev.length;
            el.value = withComma;
            const newPos = Math.max(0, caret + diff);
            try { el.setSelectionRange(newPos, newPos); } catch (_) {}
        });
        inputEl.dataset.bound = '1';
    }

    // ---------- 모달 열기/닫기 ----------
    function openInvoiceModal({ userName = '', reason = '', matchingId = 0 } = {}) {
        const modalEl  = $('#invoiceModal');
        if (!modalEl) return console.error('[invoice] #invoiceModal 없음');

        // 모달에 matchingId 보관
        const mid = Number(matchingId || 0);
        modalEl.dataset.matchingId = mid ? String(mid) : '';

        const userEl   = $('#invUser', modalEl);
        const titleEl  = $('#invTitle', modalEl);
        const amountEl = $('#invAmount', modalEl);
        const dueEl    = $('#invDue', modalEl);
        const memoEl   = $('#invMemo', modalEl);

        if (userEl)  userEl.value = userName || '';
        if (titleEl) titleEl.value = reason ? `${reason} 상담료` : '';
        if (amountEl) amountEl.value = '';
        if (dueEl)   dueEl.value = '';
        if (memoEl)  memoEl.value = '';

        bindAmountFormatter(amountEl);

        // 열기
        modalEl.setAttribute('aria-hidden', 'false');
        (titleEl || amountEl || dueEl || memoEl)?.focus();
    }

    function closeInvoiceModal() {
        const modalEl = $('#invoiceModal');
        if (!modalEl) return;
        modalEl.setAttribute('aria-hidden', 'true');
    }

    window.openInvoiceModal = openInvoiceModal;
    window.closeInvoiceModal = closeInvoiceModal;

    // ---------- 열기 트리거 ----------
    // 리스트의 "청구서" 버튼: data-open-invoice, data-matching-id, data-user-name, data-reason
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-open-invoice]');
        if (!btn) return;
        e.preventDefault();
        openInvoiceModal({
            userName:  btn.dataset.userName || '',
            reason:    btn.dataset.reason || '',
            matchingId:Number(btn.dataset.matchingId || 0),
        });
    });

    // ---------- 닫기 ----------
    document.addEventListener('click', (e) => {
        const modalEl = $('#invoiceModal');
        if (!modalEl) return;
        if (e.target === $('.modal__backdrop', modalEl) || e.target?.dataset?.close === 'invoice') {
            e.preventDefault();
            closeInvoiceModal();
        }
    });

    // ---------- 발행(생성 → 즉시 발송) ----------
    document.addEventListener('click', async (e) => {
        if (e.target?.id !== 'invoiceSubmitBtn') return;
        e.preventDefault();

        const modalEl  = $('#invoiceModal');
        if (!modalEl) return;

        // 모달에 저장해 둔 matchingId 사용
        const matchingId = Number(modalEl.dataset.matchingId || 0);
        if (!matchingId) {
            alert('matchingId가 없습니다.');
            return;
        }

        const titleEl  = $('#invTitle', modalEl);
        const amountEl = $('#invAmount', modalEl);
        const dueEl    = $('#invDue', modalEl);
        const memoEl   = $('#invMemo', modalEl);

        const amountTotal = parseAmount(amountEl?.value);
        if (amountTotal <= 0) {
            alert('금액을 입력하세요.');
            amountEl?.focus();
            return;
        }

        const dueAt = (dueEl?.value ? new Date(`${dueEl.value}T00:00:00+09:00`).toISOString() : null);

        const payload = {
            matchingId,
            title: (titleEl?.value || '').trim() || '상담료 청구서',
            description: (memoEl?.value || '').trim(),
            amountTotal,
            dueAt
        };

        try {
            // 1) DRAFT 생성
            const res = await fetch('/api/invoices', {
                method:'POST',
                headers:{'Content-Type':'application/json','Accept':'application/json'},
                body: JSON.stringify(payload)
            });
            const created = await res.json().catch(()=> ({}));
            if (!res.ok) throw created;

            // 2) SENT로 발송
            const res2 = await fetch(`/api/invoices/${created.invoiceId}/send`, {
                method:'POST',
                headers:{'Content-Type':'application/json','Accept':'application/json'},
                body: JSON.stringify({ dueAt })
            });
            const sent = await res2.json().catch(()=> ({}));
            if (!res2.ok) throw sent;

            alert('청구서를 발송했습니다.');
            closeInvoiceModal();

            // 전문가 목록 갱신 훅이 있으면 호출
            if (typeof window.refreshInvoiceListForExpert === 'function') {
                window.refreshInvoiceListForExpert();
            }
        } catch (err) {
            console.error(err);
            alert('청구서 발행/발송 실패: ' + (err?.message || JSON.stringify(err)));
        }
    });
})();
