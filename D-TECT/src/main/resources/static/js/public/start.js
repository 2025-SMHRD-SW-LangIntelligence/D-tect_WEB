/**
 * 사진 바깥에서 파스텔 말풍선이 랜덤하게 떠다니도록 배치
 */
const messages = [
    "누군가의 말보다 당신의 마음이 더 소중해요",
    "당신은 이루 말할 수 없이 소중한 사람이에요",
    "당신을 이해하고 응원하는 사람들이 있어요",
    "혼자가 아니에요. 우리는 당신 편이에요",
    "그들의 말보다 당신의 가능성이 훨씬 커요",
    "고생 많았어요, 지금부터 우리가 도와줄게요",
    "당신의 일상을 다시 환하게 비춰요",
    "앞으로 다가올 날들은 지금과는 다를거에요",
    "더이상 당신을 아프게 두지 않을게요",
    "새로운 날개를 달고 새로운 세상으로 날아가 봐요",
    "당신의 오늘이 즐거운 하루가 되었으면 좋겠어요",
    "우리 함께 이겨내 봐요",
    "당신이 느낀 고통만큼 당신에게 행복이 왔으면 좋겠어요",
    "용기내 봐요 이겨내 봐요 ",
    "혼자라고 생각하지 말고 주위를 둘러보세요",
    "스스로를 믿어보세요",
    "오늘은 더 많이 웃는날이 되었으면 좋겠어요",
    "당신은 할 수 있어요",
    "남들이 뭐라 하더라도 당신은 당신이에요",
    "오늘 하루도 정말 수고 많으셨어요"
];

const cloud = document.getElementById("cloud");
const picture = document.querySelector(".picture");

function center() {
    return { x: window.innerWidth / 2, y: window.innerHeight / 2 };
}

function spawnCloud() {
    cloud.innerHTML = "";

    const { x: cx, y: cy } = center();

    // 중앙 이미지 위치/크기
    const rect = picture.getBoundingClientRect();
    const imgW = rect.width;
    const imgH = rect.height;

    // 사진 바깥 고리(안쪽/바깥쪽) 반지름
    const safeR = Math.max(imgW, imgH) * 0.75; // 최소 거리
    const outerR = Math.min(window.innerWidth, window.innerHeight) * 0.48; // 최대 거리

    messages.forEach((text, i) => {
        const pill = document.createElement("div");
        pill.className = "pill";
        pill.textContent = text;
        pill.dataset.tone = (i % 4) + 1;

        // 균등 분포 + 소량 난수
        const golden = 137.5 * (Math.PI / 180);
        const angle = i * golden + (Math.random() * 0.5 - 0.25);

        // 반지름 랜덤
        const r = safeR + Math.random() * (outerR - safeR);

        // 좌표
        const x = cx + r * Math.cos(angle);
        const y = cy + r * Math.sin(angle);

        pill.style.left = `${x}px`;
        pill.style.top = `${y}px`;

        // 애니메이션 타이밍 랜덤
        pill.style.animationDelay = `${(i * 0.6) % 3.2}s`;
        pill.style.animationDuration = `${7 + (Math.random() * 2 - 1)}s`;

        cloud.appendChild(pill);
    });
}

// 초기 생성 + 리사이즈 대응
spawnCloud();
window.addEventListener("resize", () => {
    clearTimeout(window.__cloudTimer);
    window.__cloudTimer = setTimeout(spawnCloud, 120);
});
