import { validatePasswords, setupPhoneValidation, checkUsername, setupEmailVerification, setupAddressSearch } from '/js/public/common.js';

const btnSubmit = document.getElementById('btnSubmit');

document.addEventListener('DOMContentLoaded', () => {
    const termsAgreeEls = document.getElementsByName('termsAgree');
    const formData = {};

    // 약관 동의 체크
    termsAgreeEls.forEach(el => el.addEventListener('change', () => {
        btnSubmit.disabled = ![...termsAgreeEls].some(e => e.checked && e.value === 'Y');
    }));

    // hidden input에 sessionStorage 데이터 채우기
    ['name','username','password','email','phone','address','addrDetail'].forEach(id => {
        const input = document.getElementById(id);
        if(input) input.value = sessionStorage.getItem(id) || '';
    });
});

btnSubmit.addEventListener('click', async () => {
    const form = document.getElementById('termsForm');
    const fd = new FormData(form);

    try {
        const res = await fetch('/api/members/signup', {
            method: 'POST',
            body: fd
        });

        const data = await res.json();
        if(data.success){
            alert('회원가입 성공!');
            window.location.href = '/';
        } else {
            alert(`회원가입 실패: ${data.message}`);
        }
    } catch(err){
        console.error(err);
        alert('서버 통신 오류');
    }
});
