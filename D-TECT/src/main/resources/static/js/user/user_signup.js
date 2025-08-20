import { validatePasswords, setupPhoneValidation, checkUsername, setupEmailVerification, setupAddressSearch } from '/js/public/common.js';

const password = document.getElementById('password');
const password2 = document.getElementById('password2');
const pwdMsg = document.getElementById('pwdMsg');
const phone = document.getElementById('phone');
const username = document.getElementById('username');
const email = document.getElementById('email');
const emailCode = document.getElementById('emailCode');
const emailMsg = document.getElementById('emailMsg');
const btnCheckId = document.getElementById('btnCheckId');
const btnEmail = document.getElementById('btnEmail');
const btnVerifyEmail = document.getElementById('btnVerifyEmail');
const btnAddr = document.getElementById('btnAddr');
const addressEl = document.getElementById('address');

validatePasswords(password, password2, pwdMsg, true);
setupPhoneValidation(phone);
checkUsername(btnCheckId, username);
setupEmailVerification(btnEmail, btnVerifyEmail, email, emailCode, emailMsg);
setupAddressSearch(btnAddr, addressEl);

// Step1 -> Step2 저장
document.getElementById('goTerms').addEventListener('click', ()=>{
    if(!validatePasswords(password, password2, pwdMsg)) { password2.reportValidity(); return; }
    if(!/^[0-9]{10,11}$/.test(phone.value)){ phone.reportValidity(); return; }
    
    const data = {
        username: username.value.trim(),
        password: password.value.trim(),
        name: document.getElementById('name').value.trim(),
        email: email.value.trim(),
        phone: phone.value.trim(),
        address: addressEl.value.trim(),
        addrDetail: document.getElementById('addrDetail').value.trim()
    };
    sessionStorage.setItem('userSignupDraft', JSON.stringify(data));
    location.href='/userTermPage';
});
