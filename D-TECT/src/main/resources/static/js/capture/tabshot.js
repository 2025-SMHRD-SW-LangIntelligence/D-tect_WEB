// ===== 요소 =====
const els = {
  reselectBtn: document.getElementById('reselectBtn'),
  resetBtn   : document.getElementById('resetBtn'),
  startBtn   : document.getElementById('startBtn'),
  stopBtn    : document.getElementById('stopBtn'),
  intervalSec: document.getElementById('intervalSec'),
  intervalNum: document.getElementById('intervalNum'),
  saveMode   : document.getElementById('saveMode'),
  useFs      : document.getElementById('useFs'),

  prefix     : document.getElementById('prefix'),
  statusWrap : document.getElementById('statusWrap'),
  statusDot  : document.getElementById('statusDot'),
  statusText : document.getElementById('statusText'),
  video      : document.getElementById('video'),
  canvas     : document.getElementById('canvas'),
  log        : document.getElementById('log'),
  gotoBtn    : document.getElementById('gotoResultBtn')
};

// ===== 상태 =====
let stream = null, timerId = null, dirHandle = null, busy = false;
let elapsedTimer = null, elapsedSec = 0, captureIdx = 0;
let selecting = false;

// 저장 전략: 'folder' | 'download'
let saveStrategy = 'folder';

// ====== 분석(Analysis) 연동 상태 ======
let analysisId = null;

// ===== 유틸 =====
const clamp = (n,min,max)=> Math.min(Math.max(n,min),max);
const fmtTime = (sec)=>{ const h=Math.floor(sec/3600),m=Math.floor((sec%3600)/60),s=sec%60; const mm=String(m).padStart(2,'0'), ss=String(s).padStart(2,'0'); return h>0?`${String(h).padStart(2,'0')}:${mm}:${ss}`:`${mm}:${ss}`; };
function setStatus(kind,text){
  els.statusDot.className = `dot ${kind}`;
  els.statusText.textContent = text;
  els.statusText.title = text || '';
}
function log(msg){ const line = `[${new Date().toLocaleTimeString()}] ${msg}\n`; els.log.textContent += line; els.log.scrollTop = els.log.scrollHeight; }

// ▶ 파일명용 타임스탬프(초 단위, 밀리초 없음)
function ts(){
  const d = new Date();
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}_` +
      `${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`;
}

function ensureElapsed(){
  if (document.getElementById('elapsed')) return;
  const s=document.createElement('span');
  s.id='elapsed'; s.className='elapsed'; s.textContent='00:00';
  els.statusWrap.appendChild(s);
}
function hideElapsed(){
  const s=document.getElementById('elapsed'); if (s) s.remove();
}

function currentUserId(){
  const v = document.querySelector('meta[name="user-id"]')?.content;
  return v ? parseInt(v, 10) : null;
}

// ===== 서버 통신(Analysis 시작/종료) =====
async function notifyStart(){
  try{
    const uid = currentUserId();
    if (!uid){ log('userId 메타가 없습니다. 분석 생성 생략'); return; }
    const res = await fetch('/api/analysis/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: uid })
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json();
    analysisId = json.analId;
    log(`분석 생성됨 #${analysisId} (startedAt=${json.startedAt})`);
  }catch(e){
    log(`분석 생성 실패: ${e.message}`);
  }
}

async function notifyFinish(){
  if (!analysisId) return;
  try{
    const res = await fetch(`/api/analysis/${analysisId}/finish`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json();
    log(`분석 종료됨 #${json.analId} (finishedAt=${json.finishedAt || 'null'})`);
  }catch(e){
    log(`분석 종료 실패: ${e.message}`);
  } finally {
    analysisId = null;
  }
}

// ===== 페이지 이탈시 finish 보장(sendBeacon) =====
function finishBeacon(){
  if (!analysisId) return;
  try{
    const url = `/api/analysis/${analysisId}/finish`;
    navigator.sendBeacon(url, new Blob([], {type:'text/plain'}));
  }catch(e){}
}
window.addEventListener('pagehide', finishBeacon, {capture:true});
window.addEventListener('beforeunload', finishBeacon);

// ===== 폴더 권한/전략 =====
async function verifyDirPermission(handle){
  if (!handle) return false;
  let perm = await handle.queryPermission?.({ mode: 'readwrite' });
  if (perm === 'granted') return true;
  if (perm === 'prompt') perm = await handle.requestPermission?.({ mode: 'readwrite' });
  return perm === 'granted';
}

function confirmDownloadFallback(){
  return window.confirm(
      '이 환경에서는 PC 폴더에 직접 저장할 수 없습니다.\n' +
      '대신 브라우저 다운로드 방식으로 저장할까요?\n\n' +
      '확인: 다운로드로 진행 / 취소: 캡처 취소'
  );
}

function gotoResults(){
  const url = els.gotoBtn?.dataset?.href || '/analysis';
  const capturing = !!(timerId || stream);
  if (capturing){
    const ok = window.confirm('캡처가 진행 중입니다. 이동하면 중지됩니다. 이동할까요?');
    if (!ok) return;
    stop('페이지 이동');
  }
  location.href = url;
}

// 기본 = 폴더 저장. 불가하면 모달 안내 후 동의 시 download, 아니면 취소
async function ensureStorageStrategy(){
  saveStrategy = 'folder';
  const supportsFS = 'showDirectoryPicker' in window;

  if (supportsFS){
    if (await verifyDirPermission(dirHandle)) return true;
    try{
      dirHandle = await window.showDirectoryPicker({ mode:'readwrite' });
      return await verifyDirPermission(dirHandle);
    }catch(e){
      log(`폴더 선택 취소/거부: ${e.message}`);
      const ok = confirmDownloadFallback();
      if (ok){ saveStrategy = 'download'; return true; }
      return false;
    }
  }else{
    const ok = confirmDownloadFallback();
    if (ok){ saveStrategy = 'download'; return true; }
    return false;
  }
}

// ===== 저장 =====
async function saveDownload(blob, fileName){
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href=url; a.download=fileName; a.style.display='none';
  document.body.appendChild(a); a.click();
  setTimeout(()=>{ URL.revokeObjectURL(url); a.remove(); },0);
  setStatus('ok', `${fileName} 저장(다운로드)`);
}

async function saveLocal(blob, fileName){
  if (!(await verifyDirPermission(dirHandle))){
    setStatus('warn','폴더 권한이 없습니다. [대상 선택] 또는 폴더 재선택이 필요합니다.');
    log('폴더 권한 없음 → 저장 중단');
    return;
  }
  try{
    const file = await dirHandle.getFileHandle(fileName,{create:true});
    const w = await file.createWritable();
    await w.write(blob); await w.close();
    setStatus('ok', `${fileName} 저장(폴더)`);
  }catch(e){
    log(`폴더 저장 실패: ${e.message}`);
    setStatus('err','폴더 저장 실패');
  }
}

// === 불링 감지 전용 폴더 저장 ===
async function saveToAlertsFolder(blob, fileName){
  if (saveStrategy !== 'folder') return; // download 모드면 스킵
  try{
    const sub = await dirHandle.getDirectoryHandle('D-tect_Alerts', {create:true});
    const file = await sub.getFileHandle(fileName, {create:true});
    const w = await file.createWritable();
    await w.write(blob); await w.close();
    log(`⚠️ 불링 감지 → D-tect_Alerts/${fileName} 추가 저장`);
  }catch(e){
    log(`Alerts 폴더 저장 실패: ${e.message}`);
  }
}

// ===== 서버로 프레임 전송 =====
async function sendToBackend(blob, fileName){
  if (!analysisId) return {flagged:false, labels:[]};
  try{
    const fd = new FormData();
    fd.append('file', blob, fileName);
    fd.append('analId', String(analysisId));

    const res = await fetch('/api/capture/frame', { method:'POST', body: fd });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json(); // {flagged:boolean, labels:[...]}
    return json;
  }catch(e){
    log(`백엔드 전송 실패: ${e.message}`);
    return {flagged:false, labels:[]};
  }
}

// ===== 경고만(자동 종료 없음) =====
const ANOMALY = {
  ENABLED: true,
  STALL_MS: 6000,
  HARD_STALL_MS: 15000,
  MUTE_GRACE_MS: 7000,
  PIXEL_SAMPLE_MS: 1000,
  RVFC_FALLBACK_MS: 4000
};
const LOG_VERBOSE_MUTE = false;

// ===== 최소화/가림 추정 + 스톨 감지(경고만) =====
let minWatchTimer = null;
let rvfcLoopActive = false;
let lastFrameAt = 0;
let minimizedLikely = false;
let mutedSince = null;

const sampleCanvas = document.createElement('canvas');
sampleCanvas.width = 48; sampleCanvas.height = 27;
const sampleCtx = sampleCanvas.getContext('2d', { willReadFrequently: true });
let lastSampleHash = null;

function hashFrame(v){
  try{
    sampleCtx.drawImage(v, 0, 0, sampleCanvas.width, sampleCanvas.height);
    const d = sampleCtx.getImageData(0,0,sampleCanvas.width,sampleCanvas.height).data;
    let acc = 0; for (let i=0; i<d.length; i+=16){ acc = (acc*31 + d[i] + d[i+1] + d[i+2])|0; }
    return acc;
  }catch{ return null; }
}

function updateMinState(flag){
  if (flag === minimizedLikely) return;
  minimizedLikely = !!flag;
  let b = document.getElementById('minBadge');
  if (!b){
    b = document.createElement('span');
    b.id='minBadge'; b.className='badge'; b.style.marginLeft='8px';
    els.statusWrap.appendChild(b);
  }
  if (flag){ b.textContent='프레임 지연/가림 추정'; b.style.background='#fff5f5'; b.style.color='#b91c1c'; }
  else     { b.textContent='정상 프레임';     b.style.background='#f7faff'; b.style.color='#556685'; }
}

function startMinimizeWatch(stream){
  stopMinimizeWatch();
  const v = els.video;
  lastFrameAt = performance.now();
  minimizedLikely = false; updateMinState(false);
  lastSampleHash = null;
  mutedSince = null;

  rvfcLoopActive = true;
  if (v.requestVideoFrameCallback){
    const loop = ()=> v.requestVideoFrameCallback((now)=>{
      lastFrameAt = now;
      updateMinState(false);
      if (rvfcLoopActive) loop();
    });
    loop();
  }

  const pixelTimer = setInterval(()=>{
    const gap = performance.now() - lastFrameAt;
    if (gap > ANOMALY.RVFC_FALLBACK_MS){
      const h = hashFrame(v);
      if (h !== null){
        if (lastSampleHash !== null && h !== lastSampleHash){ lastFrameAt = performance.now(); }
        lastSampleHash = h;
      }
    }
  }, ANOMALY.PIXEL_SAMPLE_MS);

  const track = stream?.getVideoTracks?.()[0];
  if (track){
    track.onended = ()=> handleTrackEnded();
    track.onmute  = ()=>{
      if (LOG_VERBOSE_MUTE) log('트랙 mute 감지(일시적일 수 있음)');
      if (!mutedSince) mutedSince = performance.now();
    };
    track.onunmute = ()=>{
      if (LOG_VERBOSE_MUTE) log('트랙 unmute');
      mutedSince = null;
    };
  }

  minWatchTimer = setInterval(()=>{
    const now = performance.now();
    const sinceFrame = now - lastFrameAt;

    if (sinceFrame > ANOMALY.HARD_STALL_MS){
      setStatus('warn', '프레임 중단 추정');
      log('[경고] 프레임 완전 중단(HARD_STALL)');
      return;
    }

    const softStall = sinceFrame > ANOMALY.STALL_MS;
    if (softStall){
      updateMinState(true);
      const mutedLong = mutedSince && (now - mutedSince) > ANOMALY.MUTE_GRACE_MS;
      if (mutedLong){
        setStatus('warn', 'mute+프레임 스톨 지속');
        log('[경고] mute+프레임 스톨 지속');
      }
    } else {
      updateMinState(false);
    }
  }, 1000);

  startMinimizeWatch._pixelTimer = pixelTimer;
}

function stopMinimizeWatch(){
  if (minWatchTimer) clearInterval(minWatchTimer);
  if (startMinimizeWatch._pixelTimer) clearInterval(startMinimizeWatch._pixelTimer);
  rvfcLoopActive = false;
  minWatchTimer = null;
  mutedSince = null;
  updateMinState(false);
}

// 공유 중지 시: UI만 정리(세션 유지)
async function handleTrackEnded(){
  log('공유가 중지되었습니다. (세션 유지, 재시작 가능)');
  await cleanupWithoutFinish('공유 중지됨');
}

// ===== 캡처 로직 =====
async function chooseTarget(){
  try{
    setStatus('idle','대상 선택 중…');
    return await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
  }catch(e){
    log(`대상 선택 취소/거부: ${e.name || 'Error'} - ${e.message}`);
    setStatus('idle','대기');
    return null;
  }
}

async function initStream(newStream){
  if (stream){ try{ stream.getTracks().forEach(t=>t.stop()); }catch{} }
  stream = newStream;
  els.video.srcObject = stream;
  await els.video.play().catch(()=>{});
  await new Promise(r=>{ if (els.video.readyState >= 2) r(); else els.video.onloadedmetadata = r; });
  startMinimizeWatch(stream);
}

let lastStamp = null; // 같은 초 판별용

async function start(){
  if (busy) return; busy = true;
  try{
    const ok = await ensureStorageStrategy();
    if (!ok){ setStatus('idle','대기'); busy=false; return; }

    if (!stream){
      const s = await chooseTarget();
      if (!s){ busy=false; return; }
      await initStream(s);
    }

    if (!analysisId){
      await notifyStart();
    } else {
      log(`기존 분석 세션 재사용 #${analysisId}`);
    }

    captureIdx = 0;
    lastStamp  = null;

    ensureElapsed(); elapsedSec=0; document.getElementById('elapsed').textContent='00:00';
    if (elapsedTimer) clearInterval(elapsedTimer);
    elapsedTimer = setInterval(()=>{ elapsedSec++; document.getElementById('elapsed').textContent = fmtTime(elapsedSec); }, 1000);

    const sec = currentInterval();
    if (timerId) clearInterval(timerId);
    timerId = setInterval(captureOnce, sec*1000);
    await captureOnce();

    els.startBtn.disabled = true; els.stopBtn.disabled = false;
    els.intervalNum.disabled = true; els.intervalSec.disabled = true;
    els.saveMode?.setAttribute('disabled','');
    els.useFs?.setAttribute('disabled','');
    els.prefix.disabled = true;
    els.reselectBtn.disabled = true;

    setStatus('ok', `캡처 중 (${saveStrategy === 'folder' ? '폴더 저장' : '다운로드'})`);
    log(`캡처 시작 (주기 ${sec}초, 전략=${saveStrategy})`);
  }catch(e){
    log(`캡처 시작 오류: ${e.message}`);
    setStatus('err','오류');
  } finally { busy=false; }
}

async function captureOnce(){
  if (!stream) return;
  const v = els.video; const w=v.videoWidth, h=v.videoHeight; if (!w||!h) return;
  els.canvas.width=w; els.canvas.height=h;
  const ctx=els.canvas.getContext('2d', {willReadFrequently:true});
  ctx.drawImage(v,0,0,w,h);
  const blob = await new Promise(r=> els.canvas.toBlob(r,'image/png'));

  const stamp = ts();
  if (stamp !== lastStamp){
    lastStamp = stamp;
    captureIdx = 1;
  } else {
    captureIdx += 1;
  }
  const suffix = (captureIdx > 1) ? `_${String(captureIdx).padStart(2,'0')}` : '';
  const name   = `${(els.prefix.value || 'capture_')}${stamp}${suffix}.png`;

  // 1) 로컬/다운로드 저장
  if (saveStrategy === 'folder') await saveLocal(blob, name);
  else                           await saveDownload(blob, name);

  // 2) 백엔드로 전송 → 모델 → flagged 여부 수신
  const res = await sendToBackend(blob, name);
  if (res?.flagged){
    await saveToAlertsFolder(blob, name); // 3) 감지되면 Alerts 하위 폴더에 추가 저장
  }
}

// UI/타이머/스트림만 정리(분석 finish는 호출 안 함)
async function cleanupWithoutFinish(msg){
  if (timerId){ clearInterval(timerId); timerId=null; }
  if (elapsedTimer){ clearInterval(elapsedTimer); elapsedTimer=null; }
  hideElapsed();
  stopMinimizeWatch();
  if (stream){ try{ stream.getTracks().forEach(t=>t.stop()); }catch{} stream=null; }
  els.video.srcObject=null;

  els.startBtn.disabled=false; els.stopBtn.disabled=true;
  els.intervalNum.disabled=false; els.intervalSec.disabled=false;
  els.saveMode?.removeAttribute('disabled');
  els.useFs?.removeAttribute('disabled');
  els.prefix.disabled=false;
  els.reselectBtn.disabled=false;

  setStatus('idle', msg||'중지됨');
  log('캡처 중지');
}

// 사용자 종료 버튼/의도적 이동에서만 finished_at 기록
async function stop(msg){
  await cleanupWithoutFinish(msg);
  await notifyFinish();
}

async function reselect(){
  if (selecting) return;
  selecting = true;
  els.reselectBtn.disabled = true;
  try{
    const s = await chooseTarget();
    if (!s) return;
    await initStream(s);
    setStatus('idle','대상 변경됨');
    log('대상 재선택 완료');
  } finally {
    selecting = false;
    els.reselectBtn.disabled = false;
  }
}

function currentInterval(){ const n=parseInt(els.intervalNum.value,10); return clamp(isNaN(n)?5:n,1,60); }
function applyInterval(n){ const v=clamp(n,1,60); els.intervalNum.value=String(v); els.intervalSec.value=String(v); if (timerId){ clearInterval(timerId); timerId=setInterval(captureOnce, v*1000); log(`주기 변경: ${v}초`);} }

// 숫자 입력 정제 & 슬라이더 동기화
els.intervalSec.addEventListener('input', e=>{ const v=clamp(parseInt(e.target.value,10)||5,1,60); els.intervalNum.value=String(v); if (timerId){ clearInterval(timerId); timerId=setInterval(captureOnce, v*1000); log(`주기 변경: ${v}초`);} });
els.intervalNum.addEventListener('keydown', e=>{ const ok=['Backspace','Delete','ArrowLeft','ArrowRight','Tab','Home','End']; if (ok.includes(e.key)) return; if (e.ctrlKey||e.metaKey) return; if (!/^[0-9]$/.test(e.key)) e.preventDefault(); });
els.intervalNum.addEventListener('beforeinput', e=>{ if (e.inputType.startsWith('delete')) return; const d=e.data; if (d && /\D/.test(d)) e.preventDefault(); });
els.intervalNum.addEventListener('paste', e=>{ const t=(e.clipboardData||window.clipboardData).getData('text')||''; const only=t.replace(/\D+/g,''); if (only!==t){ e.preventDefault(); if (only) document.execCommand('insertText',false,only); } });
els.intervalNum.addEventListener('input', e=>{ let v=e.target.value; if (/\D/.test(v)){ v=v.replace(/\D+/g,''); e.target.value=v; } if (v==='') return; if (/^0+$/.test(v)){ e.target.value='1'; applyInterval(1); return; } const num=parseInt(v,10); if (num>60){ e.target.value='60'; applyInterval(60); return; } els.intervalSec.value=String(num); if (timerId){ clearInterval(timerId); timerId=setInterval(captureOnce, num*1000); log(`주기 변경: ${num}`);} });

// 버튼 타입 보정
function ensureButtonsClickable(){
  try{
    els.reselectBtn?.setAttribute('type','button');
    els.resetBtn?.setAttribute('type','button');
    els.startBtn?.setAttribute('type','button');
    els.stopBtn?.setAttribute('type','button');
    if (els.reselectBtn) els.reselectBtn.disabled = false;
  }catch{}
}

// ===== 이벤트 바인딩
if (els.gotoBtn) els.gotoBtn.addEventListener('click', gotoResults);
els.reselectBtn.addEventListener('click', reselect);
els.resetBtn.addEventListener('click', ()=>{ stop('리셋'); els.log.textContent=''; setStatus('idle','대기'); updateMinState?.(false); });
els.resetBtn.addEventListener('mousedown', ()=>{ const t=setTimeout(()=> location.reload(), 2000); const cancel=()=>{ clearTimeout(t); window.removeEventListener('mouseup', cancel); window.removeEventListener('mouseleave', cancel); }; window.addEventListener('mouseup', cancel); window.addEventListener('mouseleave', cancel); });
els.startBtn.addEventListener('click', start);
els.stopBtn.addEventListener('click', ()=> stop());
window.addEventListener('keydown', e=>{ const isCmdR = (e.key.toLowerCase()==='r') && (e.metaKey||e.ctrlKey); if (isCmdR){ e.preventDefault(); stop('리셋'); setTimeout(()=> location.reload(), 50);} });

// 초기화
applyInterval(5);
setStatus('idle','대기');
ensureButtonsClickable();
updateMinState(false);
log('준비 완료 — [대상 선택]으로 화면/창/탭을 선택하세요. 기본 저장: PC 폴더. 불가 시 모달 동의 후 다운로드로 진행합니다.');
