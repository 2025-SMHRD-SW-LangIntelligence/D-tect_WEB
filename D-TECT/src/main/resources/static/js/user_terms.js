const MAIN_PAGE = './index.html';  // 가입 완료 후 이동
const START_PAGE = './start.html';  // 취소 시 이동

const btnSubmit = document.getElementById('btnSubmit');
const btnCancel = document.getElementById('btnCancel');
const radios = document.querySelectorAll('input[name="agree"]');

radios.forEach(r =>
    r.addEventListener('change', () => {
        btnSubmit.disabled = !(document.querySelector('input[name="agree"]:checked')?.value === 'yes');
    })
);

btnSubmit.addEventListener('click', () => {
    // const draft = JSON.parse(sessionStorage.getItem('userSignupDraft') || '{}');
    // TODO: 실제 가입 API 호출
    alert('회원가입이 완료되었습니다.');
    sessionStorage.removeItem('userSignupDraft');
    location.href = MAIN_PAGE;
});

btnCancel.addEventListener('click', () => {
    if (confirm('회원가입을 취소하시겠습니까?')) {
        sessionStorage.removeItem('userSignupDraft');
        location.href = START_PAGE;
    }
});
