import { initProfileEditPopup, setupLogout, setupWithdraw } from '/js/public/common.js';

function getEndpoint() {
  const panel = document.querySelector('.bot-panel');
  let base =
      panel?.dataset?.endpoint?.trim() ||
      document.querySelector('meta[name="bot-base"]')?.content?.trim() ||
      '';

  if (base) {
    if (/\/message$/.test(base)) return base;
    return base.replace(/\/+$/, '') + '/message';
  }

  // 2) 로컬 개발 환경
  const host = location.hostname;
  const isLocal = host === 'localhost' || host === '127.0.0.1';
  if (isLocal) return 'http://127.0.0.1:8002/api/bot/message';

  // 3) 배포(nginx)
  return '/api/bot/message';
}

export async function sendToBot(text, context = {}) {
  const session = sessionStorage.getItem('botSession') ?? crypto.randomUUID();
  const body = {
    sessionId: session,
    message: text,
    history: [],
    context: { page: location.pathname, ...context }
  };

  sessionStorage.setItem('botSession', session);

  const res = await fetch(getEndpoint(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    const msg = await res.text().catch(() => '');
    throw new Error(`Bot API ${res.status}: ${msg || 'request failed'}`);
  }
  return res.json(); // { reply, citations, meta }
}

function appendMessage(target, who, text) {
  const div = document.createElement('div');
  div.className = `msg ${who}`;
  div.textContent = text;
  target.appendChild(div);
  target.scrollTop = target.scrollHeight;
}

// 페이지 로드 시 폼/버튼 연결
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('botForm');
  const input = document.getElementById('userInput');
  const log = document.getElementById('chatLog');
  const gotoBtn = document.getElementById('gotoMyPageBtn');
  if (!form || !input || !log) return;

  // 메인페이지 버튼임
  // 마이페이지 버튼 아님
  const metaMyPage = document.querySelector('meta[name="mypage-url"]')?.content?.trim();
  if (gotoBtn && metaMyPage) gotoBtn.setAttribute('href', metaMyPage);

  let isSending = false;
  let isComposing = false;

  // 한글 조합 상태 추적
  input.addEventListener('compositionstart', () => { isComposing = true; });
  input.addEventListener('compositionend', () => { isComposing = false; });

  // Enter 전송 / Shift+Enter 줄바꿈
  input.addEventListener('keydown', (e) => {
    // 조합 중이면 전송 금지 (mac/Safari 한글 마지막 글자 이슈 방지)
    if (isComposing || e.isComposing || e.keyCode === 229) return;

    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!isSending) {
        isSending = true;
        form.requestSubmit();
        // 아주 짧은 락으로 중복 전송 방지
        setTimeout(() => { isSending = false; }, 120);
      }
    }
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const text = (input.value || '').trim();
    if (!text) return;

    appendMessage(log, 'me', text);

    // 1) 즉시 비우기
    input.value = '';
    input.dispatchEvent(new Event('input')); // 프레임워크/바인딩 동기화
    // 2) 다음 렌더 틱에서 한 번 더 비우기(IME 늦은 커밋 차단)
    requestAnimationFrame(() => {
      input.value = '';
      input.setSelectionRange(0, 0);
    });

    input.focus();

    try {
      const { reply } = await sendToBot(text);
      appendMessage(log, 'bot', reply ?? '(응답 없음)');
    } catch (err) {
      appendMessage(log, 'bot', `오류: ${err.message}`);
      console.error(err);
    }
  });
});
