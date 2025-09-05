(() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  function pickAnalIdRaw() {
    const el = document.querySelector('[data-anal-id]');
    if (el?.dataset?.analId) return el.dataset.analId;
    try { const urlId = new URLSearchParams(location.search).get('analId'); if (urlId) return urlId; } catch {}
    if (typeof window !== 'undefined' && window.__analId != null) return String(window.__analId);
    try { const saved = sessionStorage.getItem('analysisAnalId'); if (saved) return saved; } catch {}
    try { const last = localStorage.getItem('analysisAnalIdLast'); if (last) return last; } catch {}
    try { const m = (location.pathname || '').match(/(?:^|\/)(\d{1,20})(?:\/|$)/); if (m) return m[1]; } catch {}
    return null;
  }
  function sanitizeAnalId(raw){ if(raw==null) return null; const s=String(raw).trim(); if(!/^\d+$/.test(s)) return null; return s; }
  function getAnalId(){ return sanitizeAnalId(pickAnalIdRaw()); }

  function ensureChartStatusEl(){
    const box = $('.chart-box'); if(!box) return null;
    let el = $('#chartStatus');
    if(!el){ el=document.createElement('div'); el.id='chartStatus'; el.className='loading-msg';
      el.innerHTML=`<div class="spinner"></div><div class="msg">분석 데이터를 불러오는 중...</div>`; box.appendChild(el); }
    return el;
  }
  function setChartStatus(type,msg){ const el=ensureChartStatusEl(); if(!el) return; if(type==='hide'){ el.remove(); return; } el.querySelector('.msg').textContent=msg||''; el.style.display='block'; }
  function renderTextFallback(labels=[],values=[]){ const box=$('.chart-box'); if(!box) return; const wrap=document.createElement('div'); wrap.className='loading-msg';
    wrap.innerHTML=`<div class="msg">차트 대신 요약 값:</div>${labels.map((l,i)=>`<div>${l}: ${Number(values[i]??0)}</div>`).join('')}`; box.appendChild(wrap); }

  async function loadSummary(analId){
    try{ const res=await fetch(`/api/analysis/${analId}/summary`,{headers:{Accept:'application/json'},credentials:'include'}); if(res.ok) return await res.json(); }catch(e){ console.warn('summary error',e); }
    return null;
  }
  async function fetchReportInfo(analId){
    try{ const res=await fetch(`/api/analysis/${analId}/report`,{headers:{Accept:'application/json'},credentials:'include'}); if(res.ok) return await res.json(); }catch{}
    return null;
  }

  function applyPdfReadyUI(reportUrl, name){
    const tile=$('#pdfTile'), caption=$('#pdfCaption'), spinner=$('#pdfSpinner');
    if(tile && caption){
      tile.hidden=false; tile.style.pointerEvents='auto'; caption.textContent='PDF 다운로드'; if(spinner) spinner.style.display='none';
      tile.addEventListener('click',e=>{ e.preventDefault(); const a=document.createElement('a'); a.href=reportUrl; a.download = name ? `${name}의 결과 보고서.pdf` : ''; document.body.appendChild(a); a.click(); a.remove(); },{once:true});
    }
  }

  function listenPdfReady(analId, onReady){
    let closed=false;
    try{
      const es=new EventSource(`/api/analysis/${analId}/events`);
      es.addEventListener('status',ev=>{ if(closed) return; try{ const data=JSON.parse(ev.data); if(data.state==='ready' && data.reportUrl){ closed=true; es.close(); onReady(data.reportUrl,data.name||null);} }catch{} });
      es.onerror=()=>{}; return ()=>{ closed=true; try{ es.close(); }catch{} };
    }catch{ return ()=>{}; }
  }

  async function pollReportReady(analId,{tries=60,intervalMs=1000}={},onReady,stopIf){
    for(let i=0;i<tries;i++){ if(stopIf()) return; const info=await fetchReportInfo(analId); if(info?.reportUrl){ if(!stopIf()) onReady(info.reportUrl,info.name||null); return; } await new Promise(r=>setTimeout(r,intervalMs)); }
    const tile=$('#pdfTile'), caption=$('#pdfCaption'), spinner=$('#pdfSpinner'); if(tile && caption){ tile.hidden=false; tile.style.pointerEvents='none'; caption.textContent='아직 준비되지 않음'; if(spinner) spinner.style.display='none'; }
  }

  function normalizeTo100(values){ const nums=values.map(v=>Number(v)||0); const max=Math.max(0,...nums); if(max<=0) return nums.map(()=>0); return nums.map(v=>Math.round((v/max)*100)); }
  function renderRadar({labels,values}){
    const canvas=$("#radar"); if(!canvas){ setChartStatus('show','차트 캔버스를 찾지 못했습니다.'); return; }
    if(typeof Chart==='undefined'){ setChartStatus('show','차트 라이브러리를 불러오지 못했습니다.'); renderTextFallback(labels,values); return; }
    const ctx=canvas.getContext('2d'); const normalized=normalizeTo100(values);
    new Chart(ctx,{ type:'radar', data:{ labels, datasets:[{ label:'검출 비율', data:normalized, fill:true, backgroundColor:'rgba(239,68,68,0.18)', borderColor:'#ef4444', borderWidth:2, pointBackgroundColor:'#ef4444', pointBorderColor:'#ef4444' }]}, options:{ responsive:true, maintainAspectRatio:false } });
    setChartStatus('hide');
  }

  (async ()=>{
    const analId=getAnalId();
    if(!analId){
      console.error('[analysis_result] analId를 찾지 못했습니다. URL ?analId=, window.__analId, sessionStorage("analysisAnalId"), data-anal-id, 혹은 경로 숫자를 제공하세요.');
      const tile=$('#pdfTile'), caption=$('#pdfCaption'), spinner=$('#pdfSpinner'); if(tile && caption){ tile.hidden=false; tile.style.pointerEvents='none'; caption.textContent='분석 ID 없음'; if(spinner) spinner.style.display='none'; }
      return;
    }
    try{ localStorage.removeItem('analysisAnalIdLast'); }catch{}

    setChartStatus('show','분석 데이터를 불러오는 중...');
    const data=await loadSummary(analId);
    if(data){
      const labels = Array.isArray(data.labels)&&data.labels.length ? data.labels : (data.counts?Object.keys(data.counts):(data.scores?Object.keys(data.scores):[]));
      const values = Array.isArray(data.values)&&data.values.length ? data.values : (data.counts?Object.values(data.counts):(data.scores?Object.values(data.scores):[]));
      if(labels?.length && values?.length) renderRadar({labels,values}); else { setChartStatus('show','표시할 분석 데이터가 없습니다.'); renderTextFallback(labels,values); }
    }else setChartStatus('show','분석 데이터를 불러오지 못했습니다.');

    let ready=false;
    const onReady=(url,name)=>{ if(ready) return; ready=true; applyPdfReadyUI(url,name); };
    const stopIf=()=>ready;

    const tile=$('#pdfTile'), caption=$('#pdfCaption'), spinner=$('#pdfSpinner');
    if(tile && caption){ tile.hidden=false; tile.style.pointerEvents='none'; caption.textContent='PDF 준비 중...'; if(spinner) spinner.style.display='inline-block'; }

    const stopSse=listenPdfReady(analId,onReady);
    pollReportReady(analId,{tries:60,intervalMs:1000},onReady,stopIf);
    window.addEventListener('pagehide',()=>{ try{ stopSse(); }catch{} },{once:true});
  })();
})();
