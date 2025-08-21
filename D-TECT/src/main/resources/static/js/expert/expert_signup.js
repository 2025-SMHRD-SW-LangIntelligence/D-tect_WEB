// ===== 경로 설정 =====
const LOGIN_PATH = "/login"; // 로고 클릭 시 이동 경로

// 로고 → 로그인
document.getElementById("logoLink").addEventListener("click", (e) => {
    e.preventDefault();
    window.location.href = LOGIN_PATH;
});

// ===== 전문분야 칩 생성/선택 =====
const SPECIALTIES = [
    "폭언·모욕·비하", "성희롱·성범죄", "직장갑질", "학교·학원 폭력",
    "온라인·사이버폭력", "명예훼손·모욕", "아동·청소년", "장애·여성·소수자"
];

const chipsWrap = document.getElementById("specialtyChips");
let selectedSpecs = new Set();

function renderChips() {
    chipsWrap.innerHTML = SPECIALTIES.map(s =>
        `<button type="button" class="chip${selectedSpecs.has(s) ? ' is-selected' : ''}" data-v="${s}">${s}</button>`
    ).join("");
}
renderChips();

chipsWrap.addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    const val = chip.dataset.v;
    if (selectedSpecs.has(val)) selectedSpecs.delete(val);
    else selectedSpecs.add(val); // 제한 두고 싶다면 여기서 크기 검사 (예: if(selectedSpecs.size>=5) return)
    renderChips();
});

// ===== 파일 업로드 =====
const fileBtn = document.getElementById("fileBtn");
const certFile = document.getElementById("certFile");
const certFileName = document.getElementById("certFileName");
fileBtn.addEventListener("click", () => certFile.click());
certFile.addEventListener("change", () => {
    certFileName.value = certFile.files?.[0]?.name || "";
});

// ===== 더미 버튼 =====
document.getElementById("checkIdBtn").addEventListener("click", () => {
    // TODO: /api/experts/check-id?uid=...
    alert("사용 가능한 아이디입니다. (예시)");
});
document.getElementById("verifyEmailBtn").addEventListener("click", () => {
    // TODO: 이메일 인증 트리거
    alert("이메일 인증 메일을 발송했습니다. (예시)");
});
document.getElementById("addrBtn").addEventListener("click", () => {
    // TODO: 주소 검색(카카오 주소 등)
    alert("주소 검색 창을 띄웁니다. (예시)");
});

// ===== 제출/검증 =====
const form = document.getElementById("signupForm");
form.addEventListener("submit", (e) => {
    e.preventDefault();

    const name = val("#name");
    const uid = val("#uid");
    const pw = val("#pw");
    const pw2 = val("#pw2");
    const email = val("#email");
    const phone = val("#phone");
    const addr = val("#addr");

    if (!name || !uid || !pw || !pw2 || !email || !phone || !addr) {
        alert("모든 필수 항목을 입력해주세요.");
        return;
    }
    if (pw.length < 8 || pw !== pw2) {
        alert("패스워드가 일치하지 않거나 8자 미만입니다.");
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        alert("올바른 이메일 주소를 입력해주세요.");
        return;
    }
    if (!/^\d{9,11}$/.test(phone)) {
        alert("전화번호는 숫자만 9~11자리로 입력해주세요.");
        return;
    }
    if (selectedSpecs.size === 0) {
        alert("전문분야를 1개 이상 선택해주세요.");
        return;
    }
    if (!(certFile.files && certFile.files.length)) {
        alert("자격증명서를 첨부해주세요.");
        return;
    }

    // 전송 payload 예시
    const payload = {
        name, uid, email, phone, addr,
        specialties: Array.from(selectedSpecs)
        // 비밀번호/파일은 실제 서비스에 맞게 전송
    };

    // TODO: 실제 가입 API 연동 (FormData 사용 권장)
    // const fd = new FormData();
    // Object.entries(payload).forEach(([k,v]) => fd.append(k, Array.isArray(v)? JSON.stringify(v) : v));
    // fd.append('password', pw);
    // fd.append('cert', certFile.files[0]);
    // const res = await fetch('/api/experts/signup', { method:'POST', body: fd });

    alert("전문가 회원가입 요청이 접수되었습니다.");
    // window.location.href = LOGIN_PATH;
});

function val(sel) { return (document.querySelector(sel).value || "").trim(); }
