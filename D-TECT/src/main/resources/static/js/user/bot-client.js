// CSRF 비활성화 상태라 헤더는 Content-Type만 있으면 충분
// 필요하면 공통 유틸을 import 가능: import { getCsrfHeaders } from '/js/public/common.js';

function getEndpoint() {
  const panel = document.querySelector('.bot-panel');
  return panel?.dataset?.endpoint || '/api/bot/message';
}

export async function sendToBot(text, context = {}) {
  const body = {
    sessionId: sessionStorage.getItem('botSession') ?? crypto.randomUUID(),
    text,
    context: { page: location.pathname, ...context }
  };
  // 세션ID를 한 번 정해두면 재사용
  sessionStorage.setItem('botSession', body.sessionId);

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

  if (!form || !input || !log) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const text = (input.value || '').trim();
    if (!text) return;

    appendMessage(log, 'me', text);
    input.value = '';
    try {
      const { reply } = await sendToBot(text);
      appendMessage(log, 'bot', reply ?? '(응답 없음)');
    } catch (err) {
      appendMessage(log, 'bot', `오류: ${err.message}`);
      // 개발 중 콘솔도 함께
      console.error(err);
    }
  });
});
