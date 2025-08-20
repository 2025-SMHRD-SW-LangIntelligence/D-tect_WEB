// expert_terms.js
document.addEventListener('DOMContentLoaded', () => {
    const termsForm = document.getElementById('termsForm');
    const btnSubmit = document.getElementById('btnSubmit');
    const btnCancel = document.getElementById('btnCancel');
    const agreeRadios = document.getElementsByName('termsAgree');

    // 1페이지 데이터 가져와서 hidden input에 채움
    const storedData = JSON.parse(sessionStorage.getItem('expertSignupData') || '{}');
    for (const key in storedData) {
        const input = document.getElementById(key);
        if (input) input.value = storedData[key];
    }

    // 약관 동의 선택 시 회원가입 버튼 활성화
    agreeRadios.forEach(radio => {
        radio.addEventListener('change', () => {
            btnSubmit.disabled = !agreeRadios[0].checked; // 동의(Y) 체크 시 활성화
        });
    });

    // 회원가입 제출
    btnSubmit.addEventListener('click', async () => {
        if (!agreeRadios[0].checked) { alert('약관에 동의해야 합니다.'); return; }

        const formData = new FormData(termsForm);

        try {
            const response = await fetch('/api/members/signup', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                alert('회원가입 완료! 로그인 페이지로 이동합니다.');
                sessionStorage.removeItem('expertSignupData');
                window.location.href = '/login';
            } else {
                alert('회원가입 실패: ' + result.message);
            }
        } catch (err) {
            console.error(err);
            alert('서버 오류가 발생했습니다.');
        }
    });

    // 회원가입 취소
    btnCancel.addEventListener('click', () => {
        if (confirm('회원가입을 취소하시겠습니까?')) {
            sessionStorage.removeItem('expertSignupData');
            window.location.href = '/';
        }
    });
});
