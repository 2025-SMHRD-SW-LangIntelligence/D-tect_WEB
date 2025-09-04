(() => {
  const $ = (sel, root = document) => root.querySelector(sel);

  function getAnalId() {
    const urlId = new URLSearchParams(location.search).get("analId");
    return urlId && /^\d+$/.test(urlId) ? urlId : null;
  }

  // summary 불러오기
  async function loadSummary(analId) {
    try {
      const res = await fetch(`/api/analysis/${analId}/summary`, {
        headers: { "Accept": "application/json" },
        credentials: "include"
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.warn("summary error", e);
    }
    return null;
  }

  // 차트 렌더링
  function renderRadar({ labels, values }) {
    const canvas = $("#radar");
    if (!canvas || typeof Chart === "undefined") return;
    const ctx = canvas.getContext("2d");

    const normalized = normalize(values);

    new Chart(ctx, {
      type: "radar",
      data: {
        labels,
        datasets: [{
          label: "검출 비율",
          data: normalized,
          fill: true,
          backgroundColor: "rgba(239,68,68,0.18)",
          borderColor: "#ef4444",
          borderWidth: 2,
          pointBackgroundColor: "#ef4444",
          pointBorderColor: "#ef4444"
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  function normalize(values) {
    const nums = values.map(v => Number(v) || 0);
    const max = Math.max(0, ...nums);
    return max <= 0 ? nums.map(() => 0) : nums.map(v => Math.round((v / max) * 100));
  }

  // presigned URL + 사용자 이름 요청
  async function fetchReportInfo(analId) {
    try {
      const res = await fetch(`/api/analysis/${analId}/report`, {
        headers: { "Accept": "application/json" },
        credentials: "include"
      });
      if (res.ok) {
        return await res.json(); // { reportUrl, expiresInDays, name }
      }
    } catch (e) {
      console.warn("report fetch error", e);
    }
    return null;
  }

  // PDF 버튼 초기화
  async function initPdfBtn(analId) {
    const tile = $("#pdfTile");
    const caption = $("#pdfCaption");
    const spinner = $("#pdfSpinner");
    if (!tile || !caption) return;

    tile.hidden = false;
    tile.removeAttribute("href");
    tile.style.pointerEvents = "none";
    caption.textContent = "PDF 준비 중...";
    spinner.style.display = "inline-block";

    let info = null;
    for (let i = 0; i < 20; i++) { // 최대 20초 폴링
      info = await fetchReportInfo(analId);
      if (info?.reportUrl) break;
      await new Promise(r => setTimeout(r, 1000));
    }

    if (info?.reportUrl) {
      caption.textContent = "PDF 다운로드";
      spinner.style.display = "none";
      tile.style.pointerEvents = "auto";

      // ✅ 여러 번 클릭해도 동작하도록 once 제거
      tile.addEventListener("click", (e) => {
        e.preventDefault();
        const a = document.createElement("a");
        a.href = info.reportUrl;

        // 사용자 이름 포함된 파일명
        const userName = info.name || "사용자";
        a.download = `${userName}님의 결과 보고서.pdf`;

        document.body.appendChild(a);
        a.click();
        a.remove();
      });
    } else {
      caption.textContent = "아직 준비되지 않음";
      spinner.style.display = "none";
    }
  }

  // 시작
  (async () => {
    const analId = getAnalId();
    if (!analId) return;

    const data = await loadSummary(analId);
    if (data && data.labels?.length) {
      renderRadar({ labels: data.labels, values: data.values });
    }

    await initPdfBtn(analId);
  })();
})();
