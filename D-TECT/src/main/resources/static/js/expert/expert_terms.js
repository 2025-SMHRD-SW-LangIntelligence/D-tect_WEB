const LOGIN_PATH = "/loginPage";              // 로고 클릭 시 이동
const SIGNUP_PATH = "/joinExpertPage";  // 동의 후 이동

// 로고 → 로그인
document.getElementById("logoLink").addEventListener("click", (e) => {
    e.preventDefault();
    window.location.href = LOGIN_PATH;
});

// 약관 동의 체크 시 버튼 활성화
const agree = document.getElementById("agree");
const goSignup = document.getElementById("goSignup");

agree.addEventListener("change", () => {
    goSignup.disabled = !agree.checked;
});

// 동의 시
sessionStorage.setItem('signup:step1', JSON.stringify({ termsAgree:'Y' }));

// 진행 버튼
goSignup.addEventListener("click", () => {
    if (!agree.checked) return;
    window.location.href = SIGNUP_PATH;
});
