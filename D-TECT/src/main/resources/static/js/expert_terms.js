// 이동 경로(필요 시 파일명만 바꿔 쓰세요)
const MAIN_PAGE = './index.html';  // 가입 완료 후 이동할 "메인 페이지"
const START_PAGE = './start.html';  // 가입 취소 시 돌아갈 "시작 화면"

const btnSubmit = document.getElementById('btnSubmit');
const btnCancel = document.getElementById('btnCancel');

const radios = document.querySelectorAll('input[name="agree"]');

// 라디오 선택 상태에 따라 가입 버튼 활성/비활성
radios.forEach(r =>
    r.addEventListener('change', () => {
        btnSubmit.disabled = !(document.querySelector('input[name="agree"]:checked')?.value === 'yes');
    })
);

// 가입 버튼 클릭
btnSubmit.addEventListener('click', () => {
    // sessionStorage에 저장된 가입 초안(전 단계) 활용 가능
    // const draft = JSON.parse(sessionStorage.getItem('expertSignupDraft') || '{}');

    // TODO: 실제 가입 API 호출 (성공 시 아래로 이동)
    // await api.signupExpert(draft);

    // 가입 성공 처리
    alert('회원가입이 완료되었습니다.');
    // 초안 데이터 제거(선택)
    sessionStorage.removeItem('expertSignupDraft');
    location.href = MAIN_PAGE;
});

// 취소 버튼 클릭
btnCancel.addEventListener('click', () => {
    if (confirm('회원가입을 취소하시겠습니까?')) {
        // 필요하면 초안 데이터 제거
        sessionStorage.removeItem('expertSignupDraft');
        location.href = START_PAGE;
    }
});
