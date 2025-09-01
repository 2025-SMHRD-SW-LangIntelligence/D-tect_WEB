/**
 * 랜덤 배치(원형 고리 X) + 서로 겹치지 않음 + 사진(.picture)과도 겹치지 않음
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
    "오늘 하루도 정말 수고 많으셨어요",
    "잠깐 멈춰 크게 숨을 쉬어보세요, 괜찮아질 거에요",
    "서두르지 않아도 돼요, 지금 이 속도도 충분해요",
    "필요할 땐 도움을 요청해도 괜찮아요",
    "당신의 이야기를 끝까지 들어줄게요",
    "작은 한 걸음이 큰 변화를 이루어 낼 거에요",
    "상처는 당신의 가치에 조금의 흠집도 낼 수 없어요",
    "당신 곁에 우리가 있어요",

];

const cloud = document.getElementById("cloud");
const picture = document.querySelector(".picture");

// 파라미터(필요 시 조정)
const GAP_BETWEEN_PILLS = 8;      // 말풍선끼리 최소 간격(px)
const MARGIN_FROM_PIC = 24;     // 사진 경계로부터 최소 이격(px)
const VIEWPORT_PADDING = 6;      // 컨테이너 가장자리 여백(px)
const MAX_TRIES_PER_PILL = 220;    // 각 말풍선 위치 샘플링 최대 횟수

function rectsIntersect(a, b) {
    return !(a.right < b.left || a.left > b.right || a.bottom < b.top || a.top > b.bottom);
}
function expandRect(r, pad) {
    return { left: r.left - pad, top: r.top - pad, right: r.right + pad, bottom: r.bottom + pad };
}

function spawnCloud() {
    if (!cloud || !picture) return;

    // 컨테이너/사진 레이아웃 준비(사진이 아직 0크기면 잠시 대기)
    const picRect = picture.getBoundingClientRect();
    const containerRect = cloud.getBoundingClientRect();
    if (containerRect.width < 20 || containerRect.height < 20 || picRect.width < 1) {
        setTimeout(spawnCloud, 120);
        return;
    }

    cloud.innerHTML = "";

    // 사진의 "금지 영역"(여유 포함)은 전역 좌표계로 계산
    const picForbidden = expandRect(picRect, MARGIN_FROM_PIC);

    // 먼저 모든 pill을 생성해 치수 측정
    const nodes = messages.map((text, i) => {
        const pill = document.createElement("div");
        pill.className = "pill";
        pill.textContent = text;
        pill.dataset.tone = (i % 4) + 1;
        pill.style.position = "absolute";
        pill.style.visibility = "hidden";
        pill.style.left = "-9999px";
        pill.style.top = "-9999px";
        cloud.appendChild(pill);
        return pill;
    });
    const dims = nodes.map(p => ({ w: p.offsetWidth, h: p.offsetHeight }));

    // 배치된 말풍선들의 "전역 좌표계 rect(패딩 포함)"을 저장
    const placed = [];

    nodes.forEach((pill, i) => {
        const { w, h } = dims[i];
        let placedOk = false;

        for (let t = 0; t < MAX_TRIES_PER_PILL; t++) {
            // 전역 좌표계에서 무작위 위치 샘플링
            const minLeft = containerRect.left + VIEWPORT_PADDING;
            const minTop = containerRect.top + VIEWPORT_PADDING;
            const maxLeft = containerRect.right - w - VIEWPORT_PADDING;
            const maxTop = containerRect.bottom - h - VIEWPORT_PADDING;

            const left = Math.round(minLeft + Math.random() * Math.max(0, maxLeft - minLeft));
            const top = Math.round(minTop + Math.random() * Math.max(0, maxTop - minTop));
            const rect = { left, top, right: left + w, bottom: top + h };

            // 1) 사진(금지영역)과 겹치면 무효
            if (rectsIntersect(rect, picForbidden)) continue;

            // 2) 기존 말풍선과 겹치면 무효 (여유: GAP_BETWEEN_PILLS)
            const rectPadded = expandRect(rect, GAP_BETWEEN_PILLS);
            let collide = false;
            for (const r0 of placed) {
                if (rectsIntersect(rectPadded, r0)) { collide = true; break; }
            }
            if (collide) continue;

            // 통과 → 컨테이너 좌표계로 변환해 배치
            const localLeft = left - containerRect.left;
            const localTop = top - containerRect.top;
            pill.style.left = `${localLeft}px`;
            pill.style.top = `${localTop}px`;
            pill.style.visibility = "visible";

            // 애니메이션 타이밍 다양화(원 코드 유지)
            pill.style.animationDelay = `${(i * 0.6) % 3.2}s`;
            pill.style.animationDuration = `${7 + (Math.random() * 2 - 1)}s`;

            placed.push(rectPadded);
            placedOk = true;
            break;
        }

        // 실패 시 화면 모서리 fallback
        if (!placedOk) {
            const left = containerRect.left + VIEWPORT_PADDING + (i * 24) % Math.max(8, (containerRect.width - w - VIEWPORT_PADDING * 2));
            const top = containerRect.top + VIEWPORT_PADDING + (i * 37) % Math.max(8, (containerRect.height - h - VIEWPORT_PADDING * 2));
            pill.style.left = `${left - containerRect.left}px`;
            pill.style.top = `${top - containerRect.top}px`;
            pill.style.visibility = "visible";
        }
    });
}

// 초기 생성 + 리사이즈 대응
spawnCloud();
window.addEventListener("resize", () => {
    clearTimeout(window.__cloudTimer);
    window.__cloudTimer = setTimeout(spawnCloud, 120);
});
