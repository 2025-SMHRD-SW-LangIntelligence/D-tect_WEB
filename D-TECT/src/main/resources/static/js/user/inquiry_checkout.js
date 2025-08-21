/* ============ 설정 ============ */
// Toss 테스트/운영 키 교체
const TOSS_CLIENT_KEY = "test_ck_xxxxxxxxxxxxxxxxxxxxx";
const SUCCESS_URL = `${location.origin}/pay/success`;
const FAIL_URL = `${location.origin}/pay/fail`;

// 변호사 더미데이터(실제에선 서버/쿼리스트링으로 주입)
const LAWYER = {
    id: new URLSearchParams(location.search).get("lawyerId") || "1",
    name: "000 변호사",
    tags: ["성적발언", "성희롱", "사이버불링", "법률상담", "명예훼손", "사이버스토킹"],
    fees: [
        { code: "CONSULT_30", label: "기본상담 30분", amount: 30000 },
        { code: "CONSULT_60", label: "심화상담 60분", amount: 50000 },
        { code: "DOC_REVIEW", label: "문서 검토", amount: 70000 },
    ]
};
// 결제는 기본상담(30분) 기준으로 고정
const BASE_FEE = LAWYER.fees[0];

/* ============ UI 렌더 ============ */
document.getElementById("lawyerName").textContent = LAWYER.name;
document.getElementById("lawyerTags").innerHTML =
    LAWYER.tags.map(t => `<span class="tag"># ${t}</span>`).join("");

// 요금은 배지로만 보여줌(선택 없음)
document.getElementById("feeList").innerHTML = LAWYER.fees
    .map(f => `<span class="fee-pill"><b>${f.label}</b> ${f.amount.toLocaleString("ko-KR")}원</span>`)
    .join(" ");

// 파일 업로드
const fileBtn = document.getElementById("fileBtn");
const upload = document.getElementById("upload");
const fileNameEl = document.getElementById("fileName");
fileBtn.addEventListener("click", () => upload.click());
upload.addEventListener("change", () => fileNameEl.textContent = upload.files?.[0]?.name || "");

// 피해 유형 칩 (최대 3개)
const ISSUE_TYPES = [
    "성적발언/희롱", "사이버불링", "명예훼손/모욕", "사이버스토킹",
    "개인정보 유출", "사칭/도용", "불법촬영·유포", "협박/갈취", "기타"
];
const issueWrap = document.getElementById("issueChips");
let selectedIssues = new Set();
function renderIssues() {
    issueWrap.innerHTML = ISSUE_TYPES.map(t =>
        `<button type="button" class="chip ${selectedIssues.has(t) ? 'is-selected' : ''}" data-v="${t}">${t}</button>`
    ).join("");
}
renderIssues();
issueWrap.addEventListener("click", (e) => {
    const chip = e.target.closest(".chip"); if (!chip) return;
    const val = chip.dataset.v;
    if (selectedIssues.has(val)) selectedIssues.delete(val);
    else {
        if (selectedIssues.size >= 1) { alert("최대 1개까지 선택할 수 있습니다."); return; }
        selectedIssues.add(val);
    }
    renderIssues();
});

/* ============ 결제 ============ */
document.getElementById("submitBtn").addEventListener("click", async () => {
    const message = document.getElementById("message").value.trim();

    if (message.length < 5) {
        alert("상담 설명을 5자 이상 작성해주세요.");
        return;
    }
    if (selectedIssues.size === 0) {
        alert("상담받으실 유형(피해 유형)을 최소 1개 선택해주세요.");
        return;
    }

    // (권장) 상담 데이터/파일을 서버에 저장 → 서버가 orderId 발급
    const orderId = `order_${crypto.randomUUID()}`;
    const orderName = `${LAWYER.name} · ${BASE_FEE.label} · 채팅`;

    try {
        const tp = TossPayments(TOSS_CLIENT_KEY);
        await tp.requestPayment("카드", {
            amount: BASE_FEE.amount,       // ✅ 기본상담(30분)으로 고정 결제
            orderId,
            orderName,
            customerName: "사용자",        // 로그인 사용자 이름으로 교체
            successUrl: `${SUCCESS_URL}?orderId=${encodeURIComponent(orderId)}`,
            failUrl: FAIL_URL
        });
    } catch (err) {
        console.error(err);
        alert("결제가 진행되지 않았습니다.");
    }
});
