const els = {
    startBtn: document.getElementById('startBtn'),
    stopBtn : document.getElementById('stopBtn'),
    period  : document.getElementById('period'),
    saveMode: document.getElementById('saveMode'),
    useFs   : document.getElementById('useFs'),
    prefix  : document.getElementById('prefix'),
    preview : document.getElementById('preview'),
    counter : document.getElementById('counter'),
    status  : document.getElementById('status')
};

let stream = null;
let timerId = null;
let canvas = null, ctx = null;
let dirHandle = null;
let captureIdx = 0;
let busy = false;

function clamp(n, min, max){ return Math.min(Math.max(n, min), max); }
function ts(){
    const d = new Date();
    const pad = (n)=> String(n).padStart(2,'0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}_${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}-${String(d.getMilliseconds()).padStart(3,'0')}`;
}

async function askDirectoryIfAvailable(){
    if(els.saveMode.value !== 'local') return null;
    if(!els.useFs.checked) return null;
    if(!('showDirectoryPicker' in window)) return null;
    try{
        const h = await window.showDirectoryPicker({ mode: 'readwrite' });
        return h;
    }catch(e){
        console.warn('디렉터리 선택 취소/거부:', e);
        return null;
    }
}

async function startCapture(){
    if(timerId) clearInterval(timerId);
    captureIdx = 0; els.counter.textContent = `캡처 ${captureIdx}장`;
    els.status.textContent = '화면 선택 대기…';

    try{
        stream = await navigator.mediaDevices.getDisplayMedia({
            video: { displaySurface: 'browser' },
            audio: false
        });
    }catch(e){
        els.status.textContent = '사용자가 취소했거나 권한이 거부되었습니다.';
        return;
    }

    els.preview.srcObject = stream;
    await els.preview.play().catch(()=>{});

    await new Promise(r => {
        if(els.preview.readyState >= 2) return r();
        els.preview.onloadedmetadata = () => r();
    });

    canvas = document.createElement('canvas');
    canvas.width  = els.preview.videoWidth;
    canvas.height = els.preview.videoHeight;
    ctx = canvas.getContext('2d');

    dirHandle = await askDirectoryIfAvailable();
    if(els.saveMode.value === 'local'){
        if(els.useFs.checked && dirHandle){
            els.status.textContent = '폴더 저장 지원됨 — 주기 캡처 시작';
        }else if(els.useFs.checked && !dirHandle){
            els.status.textContent = '폴더 저장 미지원/취소 — 다운로드로 저장합니다.';
        }else{
            els.status.textContent = '다운로드 저장 — 주기 캡처 시작';
        }
    }else{
        els.status.textContent = '서버 업로드 — 주기 캡처 시작';
    }

    els.startBtn.disabled = true;
    els.stopBtn.disabled  = false;
    els.period.disabled   = true;
    els.saveMode.disabled = true;
    els.useFs.disabled    = true;
    els.prefix.disabled   = true;

    const sec = clamp(parseInt(els.period.value||'5',10), 1, 60);
    const periodMs = sec * 1000;

    timerId = setInterval(captureOnce, periodMs);
    captureOnce();

    const [track] = stream.getVideoTracks();
    track.onended = () => stopCapture('공유가 중지되어 캡처를 종료했습니다.');
}

async function captureOnce(){
    if(busy || !stream) return; busy = true;
    try{
        ctx.drawImage(els.preview, 0, 0, canvas.width, canvas.height);
        const blob = await new Promise(r => canvas.toBlob(r, 'image/png'));
        const name = `${els.prefix.value || 'capture_'}${ts()}_${(++captureIdx)}.png`;
        els.counter.textContent = `캡처 ${captureIdx}장`;

        if(els.saveMode.value === 'local'){
            await saveLocal(blob, name);
        } else {
            await uploadToServer(blob, name);
        }
    }catch(e){
        console.error(e);
        els.status.textContent = '캡처 중 오류 발생';
    }finally{
        busy = false;
    }
}

async function saveLocal(blob, fileName){
    if(dirHandle){
        try{
            const fileHandle = await dirHandle.getFileHandle(fileName, { create: true });
            const writable = await fileHandle.createWritable();
            await writable.write(blob);
            await writable.close();
            els.status.textContent = `${fileName} 저장 완료 (폴더)`;
            return;
        }catch(e){
            console.warn('폴더 저장 실패, 다운로드로 폴백', e);
        }
    }
    // 다운로드 폴백
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = fileName; a.style.display='none';
    document.body.appendChild(a); a.click();
    setTimeout(()=>{ URL.revokeObjectURL(url); a.remove(); }, 0);
    els.status.textContent = `${fileName} 다운로드 시작`;
}

async function uploadToServer(blob, fileName){
    const fd = new FormData();
    fd.append('file', new File([blob], fileName, { type: 'image/png' }));
    const res = await fetch('/api/upload', { method: 'POST', body: fd });
    if(!res.ok){
        els.status.textContent = `서버 업로드 실패 (${res.status})`;
        return;
    }
    const json = await res.json().catch(()=>({}));
    if(json && json.ok){
        els.status.textContent = `서버 저장 완료: ${json.path || fileName}`;
    }else{
        els.status.textContent = `서버 응답 오류`;
    }
}

function stopCapture(msg){
    if(timerId){ clearInterval(timerId); timerId = null; }
    if(stream){ stream.getTracks().forEach(t => t.stop()); stream = null; }
    els.startBtn.disabled = false;
    els.stopBtn.disabled  = true;
    els.period.disabled   = false;
    els.saveMode.disabled = false;
    els.useFs.disabled    = false;
    els.prefix.disabled   = false;
    els.status.textContent = msg || '중지되었습니다.';
}

els.startBtn.addEventListener('click', startCapture);
els.stopBtn .addEventListener('click', () => stopCapture());