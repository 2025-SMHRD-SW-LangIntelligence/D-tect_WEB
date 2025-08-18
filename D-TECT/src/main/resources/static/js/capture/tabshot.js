// ===== 요소 =====
const els = {
    reselectBtn: document.getElementById('reselectBtn'),
    resetBtn   : document.getElementById('resetBtn'),
    startBtn   : document.getElementById('startBtn'),
    stopBtn    : document.getElementById('stopBtn'),
    intervalSec: document.getElementById('intervalSec'),
    intervalNum: document.getElementById('intervalNum'),
    // 아래 두 UI는 있어도 되고 없어도 됩니다(이번 설계에선 기본 폴더 저장이 우선)
    saveMode   : document.getElementById('saveMode'),
    useFs      : document.getElementById('useFs'),

    prefix     : document.getElementById('prefix'),
    statusWrap : document.getElementById('statusWrap'),
    statusDot  : document.getElementById('statusDot'),
    statusText : document.getElementById('statusText'),
    video      : document.getElementById('video'),
    canvas     : document.getElementById('canvas'),
    log        : document.getElementById('log')
};

// ===== 상태 =====
let stream = null, timerId = null, dirHandle = null, busy = false;
let elapsedTimer = null, elapsedSec = 0, captureIdx = 0;
let selecting = false; // 대상 선택 중 재진입 방지

// 저장 전략: 'folder' | 'download'
// 디폴트는 폴더에 저장입니다.
let saveStrategy = 'folder';

// ===== 유틸 =====
const clamp = (n,min,max)=> Math.min(Math.max(n,min),max);
const fmtTime = (sec)=>{ const h=Math.floor(sec/3600),m=Math.floor((sec%3600)/60),s=sec%60; const mm=String(m).padStart(2,'0'), ss=String(s).padStart(2,'0'); return h>0?`${String(h).padStart(2,'0')}:${mm}:${ss}`:`${mm}:${ss}`; };
function setStatus(kind,text){ els.statusDot.className = `dot ${kind}`; els.statusText.textContent = text; }
function log(msg){ const line = `[${new Date().toLocaleTimeString()}] ${msg}\n`; els.log.textContent += line; els.log.scrollTop = els.log.scrollHeight; }
function ts(){ const d=new Date(); const pad=n=>String(n).padStart(2,'0'); return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}_${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}-${String(d.getMilliseconds()).padStart(3,'0')}`; }

function ensureElapsed(){ if (document.getElementById('elapsed')) return; const s=document.createElement('span'); s.id='elapsed'; s.className='elapsed'; s.textContent='00:00'; els.statusWrap.appendChild(s); }
function hideElapsed(){ const s=document.getElementById('elapsed'); if (s) s.remove(); }

// ===== 폴더 권한/전략 =====
async function verifyDirPermission(handle){
    if (!handle) return false;
    let perm = await handle.queryPermission?.({ mode: 'readwrite' });
    if (perm === 'granted') return true;
    if (perm === 'prompt') perm = await handle.requestPermission?.({ mode: 'readwrite' });
    return perm === 'granted';
}

function confirmDownloadFallback(){
    // 폴더 저장방식 미지원시
    // 다운로드 방식 사용자에게 선택지 지공
    return window.confirm(
        '이 환경에서는 PC 폴더에 직접 저장할 수 없습니다.\n' +
        '대신 브라우저 다운로드 방식으로 저장할까요?\n\n' +
        '확인: 다운로드로 진행 / 취소: 캡처 취소'
    );
}

// 기본 = 폴더 저장. 불가하면 모달 안내 후 동의 시 download, 아니면 취소
async function ensureStorageStrategy(){
    saveStrategy = 'folder';
    const supportsFS = 'showDirectoryPicker' in window;

    if (supportsFS){
        // 권한 OK면 그대로 폴더 저장
        if (await verifyDirPermission(dirHandle)) return true;
        // 폴더 선택 유도(최초 1회)
        try{
            dirHandle = await window.showDirectoryPicker({ mode:'readwrite' });
            return await verifyDirPermission(dirHandle);
        }catch(e){
            log(`폴더 선택 취소/거부: ${e.message}`);
            // 폴더 저장 거부/취소 → 모달로 다운로드 전환 여부 확인
            const ok = confirmDownloadFallback();
            if (ok){ saveStrategy = 'download'; return true; }
            return false;
        }
    }else{
        // 아예 지원 안 함 → 모달 안내 후 동의 시 다운로드로 진행
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
    setStatus('ok', `${fileName} 다운로드 시작`);
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

async function uploadToServer(blob, fileName){
    const fd = new FormData(); fd.append('file', new File([blob], fileName, {type:'image/png'}));
    const res = await fetch('/api/upload', {method:'POST', body: fd});
    if (!res.ok){ setStatus('err', `서버 업로드 실패(${res.status})`); return; }
    const json = await res.json().catch(()=>({}));
    if (json && json.ok){ setStatus('ok', `서버 저장 완료: ${json.path || fileName}`); }
    else setStatus('err', '서버 응답 오류');
}

// ===== 최소화/가림 추정 =====
let minWatchTimer = null;
let rvfcActive = false;
let rvfcLoopActive = false;
let lastFrameAt = 0;
let minimizedLikely = false;
let stallHits = 0, resumeHits = 0;
let lastPF = -1, lastMT = -1;
let mutedSince = null;
let muteTimer = null;

const GAP_THRESH_MS  = 60000;
const STALL_CONSEC   = 2;
const RESUME_CONSEC  = 2;
const MUTE_DEBOUNCE_MS = 5000;
const SAMPLE_EVERY_MS  = 2000;

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
    if (!b){ b = document.createElement('span'); b.id='minBadge'; b.className='badge'; b.style.marginLeft='8px'; els.statusWrap.appendChild(b); }
    if (flag){ b.textContent='최소화/가림 추정'; b.style.background='#fff5f5'; b.style.color='#b91c1c'; }
    else { b.textContent='정상 프레임'; b.style.background='#f7faff'; b.style.color='#556685'; }
}

function startMinimizeWatch(stream){
    stopMinimizeWatch();
    const v = els.video;
    lastFrameAt = performance.now();
    stallHits = 0; resumeHits = 0; minimizedLikely = false; updateMinState(false);
    lastPF = -1; lastMT = -1; lastSampleHash = null;

    if (v.requestVideoFrameCallback){
        rvfcActive = true; rvfcLoopActive = true;
        const loop = ()=> v.requestVideoFrameCallback((now, meta)=>{
            const pf = (meta && typeof meta.presentedFrames === 'number') ? meta.presentedFrames : -1;
            const mt = (meta && typeof meta.mediaTime === 'number') ? meta.mediaTime : -1;
            if ((pf !== -1 && pf !== lastPF) || (mt !== -1 && mt !== lastMT)){ lastFrameAt = now; lastPF = pf; lastMT = mt; }
            if (rvfcLoopActive) loop();
        });
        loop();
    } else {
        rvfcActive = false;
    }

    const pixelTimer = setInterval(()=>{
        if (!rvfcActive || performance.now() - lastFrameAt > SAMPLE_EVERY_MS*1.5){
            const h = hashFrame(v);
            if (h !== null){
                if (lastSampleHash !== null && h !== lastSampleHash){ lastFrameAt = performance.now(); }
                lastSampleHash = h;
            }
        }
    }, SAMPLE_EVERY_MS);

    const track = stream?.getVideoTracks?.()[0];
    if (track){
        track.onmute = ()=>{
            mutedSince = performance.now();
            clearTimeout(muteTimer);
            muteTimer = setTimeout(()=>{ stallHits = Math.max(stallHits, STALL_CONSEC); }, MUTE_DEBOUNCE_MS);
        };
        track.onunmute = ()=>{ mutedSince = null; clearTimeout(muteTimer); resumeHits = Math.max(resumeHits, RESUME_CONSEC); };
    }

    minWatchTimer = setInterval(()=>{
        const gap = performance.now() - lastFrameAt;
        if (gap > GAP_THRESH_MS){
            stallHits++; resumeHits = 0;
            if (!minimizedLikely && stallHits >= STALL_CONSEC) updateMinState(true);
        } else {
            resumeHits++; stallHits = 0;
            if (minimizedLikely && resumeHits >= RESUME_CONSEC) updateMinState(false);
        }
    }, 1000);

    startMinimizeWatch._pixelTimer = pixelTimer;
}

function stopMinimizeWatch(){
    if (minWatchTimer) clearInterval(minWatchTimer);
    if (startMinimizeWatch._pixelTimer) clearInterval(startMinimizeWatch._pixelTimer);
    rvfcLoopActive = false;
    clearTimeout(muteTimer); muteTimer = null; mutedSince = null;
    minWatchTimer = null; rvfcActive = false; stallHits = 0; resumeHits = 0;
    updateMinState(false);
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

async function start(){
    if (busy) return; busy = true;
    try{
        // (2) 기본: 폴더 저장. 불가 시 모달로 다운로드 전환.
        const ok = await ensureStorageStrategy();
        if (!ok){ setStatus('idle','대기'); busy=false; return; }

        if (!stream){
            const s = await chooseTarget();
            if (!s){ busy=false; return; }
            await initStream(s);
        }

        ensureElapsed(); elapsedSec=0; document.getElementById('elapsed').textContent='00:00';
        if (elapsedTimer) clearInterval(elapsedTimer);
        elapsedTimer = setInterval(()=>{ elapsedSec++; document.getElementById('elapsed').textContent = fmtTime(elapsedSec); }, 1000);

        const sec = currentInterval();
        if (timerId) clearInterval(timerId);
        timerId = setInterval(captureOnce, sec*1000);
        await captureOnce();

        els.startBtn.disabled = true; els.stopBtn.disabled = false;
        // 더 이상 사용자가 체크할 필요 없으니 비활성화(있다면)
        els.intervalNum.disabled = true; els.intervalSec.disabled = true;
        els.saveMode?.setAttribute('disabled','');
        els.useFs?.setAttribute('disabled','');
        els.prefix.disabled = true;
        els.reselectBtn.disabled = true;

        setStatus('ok', `캡처 중 (${saveStrategy === 'folder' ? '폴더 저장' : '다운로드'})`);
        log(`캡처 시작 (주기 ${sec}초, 전략=${saveStrategy})`);

        const [track] = stream.getVideoTracks();
        track.onended = ()=>{ log('공유가 중지되어 캡처 종료'); stop('공유 종료'); };
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
    const name = `${(els.prefix.value||'capture_')}${ts()}_${(++captureIdx)}.png`;

    if (saveStrategy === 'folder') await saveLocal(blob, name);
    else if (saveStrategy === 'download') await saveDownload(blob, name);
    else await saveDownload(blob, name); // 안전 장치
}

function stop(msg){
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

    setStatus('idle', msg||'중지됨'); log('캡처 중지');
}

async function reselect(){
    if (selecting) return;
    selecting = true;
    els.reselectBtn.disabled = true;
    try{
        const s = await chooseTarget();
        if (!s) return; // 취소/거부 시
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
els.intervalNum.addEventListener('input', e=>{ let v=e.target.value; if (/\D/.test(v)){ v=v.replace(/\D+/g,''); e.target.value=v; } if (v==='') return; if (/^0+$/.test(v)){ e.target.value='1'; applyInterval(1); return; } const num=parseInt(v,10); if (num>60){ e.target.value='60'; applyInterval(60); return; } els.intervalSec.value=String(num); if (timerId){ clearInterval(timerId); timerId=setInterval(captureOnce, num*1000); log(`주기 변경: ${num}초`);} });

// 버튼 타입 보정(폼 submit 방지)
function ensureButtonsClickable(){
    try{
        els.reselectBtn?.setAttribute('type','button');
        els.resetBtn?.setAttribute('type','button');
        els.startBtn?.setAttribute('type','button');
        els.stopBtn?.setAttribute('type','button');
        if (els.reselectBtn) els.reselectBtn.disabled = false;
    }catch{}
}

// 이벤트
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
