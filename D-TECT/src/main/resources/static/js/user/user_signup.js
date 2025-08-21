// ===== 경로 설정 =====
const LOGIN_PATH = "/login"; // 로고 클릭 시 이동할 로그인 경로

// 로고 → 로그인
document.getElementById("logoLink").addEventListener("click", (e) => {
    e.preventDefault();
    window.location.href = LOGIN_PATH;
});

// 간단한 검증 & 제출
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

    // TODO: 실제 가입 API 연동
    // await fetch('/api/users/signup', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({...})})

    alert("회원가입 요청이 접수되었습니다.");
    // window.location.href = LOGIN_PATH; // 가입 후 로그인으로 보낼 때 사용
});

function val(sel) { return (document.querySelector(sel).value || "").trim(); }

// 더미 버튼 동작
document.getElementById("checkIdBtn").addEventListener("click", () => {
    // TODO: /api/users/check-id?uid=...
    alert("사용 가능한 아이디입니다. (예시)");
});
document.getElementById("verifyEmailBtn").addEventListener("click", () => {
    // TODO: 이메일 인증 트리거
    alert("이메일 인증 메일을 발송했습니다. (예시)");
});
document.getElementById("addrBtn").addEventListener("click", () => {
    // TODO: 주소 검색(카카오 우편번호 등)
    alert("주소 검색 창을 띄웁니다. (예시)");
});
