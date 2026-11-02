# 팡팡파라팡 (PangPang) — 안드로이드 프로젝트

게임은 `app/src/main/assets/` 폴더에 있어요.
- `index.html`: 게임 규칙·레벨·화면 구성
- `redesign.css`: 화면 디자인
- `logo.webp`, `mascots.webp`: 로고와 블록 친구 그림

## APK 만들기
1. Android Studio 설치 후 **Open** → 이 `PangPang` 폴더 선택 (GitHub에서 받을 때는 **File › New › Project from Version Control** 에 `https://github.com/niaxcroknit/pangpang` 입력)
2. Gradle 동기화가 끝날 때까지 기다리기 (처음엔 몇 분 걸림)
3. 휴대폰 USB 연결(개발자 옵션 → USB 디버깅 켜기) → ▶ Run
4. 설치 파일만 필요하면: Build → Build App Bundle(s) / APK(s) → Build APK(s)

## 레벨 바꾸는 곳
`index.html` 안의 `levelCfg(n)` 함수 — 난이도 구간(10·30·60·100단계, 그 뒤 100단계마다)별로 숫자 범위, 목표, 터뜨릴 횟수, 최소 연결 수가 정해져 있어요. 구간이 바뀔 때 뜨는 안내 문구는 `TIER_NOTE`에 있어요.

## 글꼴
본문·숫자는 나눔스퀘어 네오, 제목과 캐릭터는 그림 파일(`logo.webp`, `mascots.webp`)이에요. 화면에 쓰인 글자만 골라서 `index.html` 안에 넣어 뒀기 때문에 인터넷이 없어도 글꼴이 그대로 나와요. 화면 문구를 새로 추가하면 그 글자는 기본 글꼴로 보일 수 있으니, 글꼴을 다시 만들어 넣어야 해요. (나눔 글꼴 라이선스: 상업적 무료)

## 앱에서 따로 처리하는 것 (MainActivity.java)
- **뒤로가기**: 창이 떠 있으면 창을 닫고, 게임 중이면 첫 화면으로 가요. 첫 화면에서만 앱이 닫혀요.
- **하던 판 저장**: 앱을 잠깐 나가면(전화, 홈 버튼) 하던 판을 저장하고, 다시 들어와 같은 모드를 누르면 이어서 해요.
- **다크 모드**: 폰 설정을 따라가요. 게임 안에서 밝기 버튼으로 직접 고르면 그 설정이 우선이에요. 상태바 색도 같이 바뀌어요.

## 출시용 파일 만들기 (플레이스토어용 .aab)
1. Android Studio 메뉴 **Build › Generate Signed App Bundle / APK** → **Android App Bundle** 선택
2. **Create new...** 로 서명 키(.jks)를 새로 만들어요. 비밀번호와 별칭(alias)을 정해서 입력해요.
3. **release** 를 고르고 Finish → `app/release/app-release.aab` 가 생겨요. 이 파일을 Play Console에 올려요.
4. **서명 키 파일과 비밀번호는 꼭 따로 백업해 두세요.** 잃어버리면 업데이트를 올릴 수 없어요. (Play 앱 서명을 켜 두면 Google이 배포용 키를 대신 보관해 줘요.)
5. 업데이트할 때마다 `app/build.gradle` 의 `versionCode` 를 1씩 올리고, `versionName` 도 바꿔요(예: 1.0 → 1.1).

스토어 설명, 아이콘·그래픽·스크린샷, 개인정보처리방침은 `play-store` 폴더에 있어요. 개인정보처리방침 공개 링크: https://app.notion.com/p/4024235ad3ee80b3b4ffe74ae7240d02

## 설정과 점수
- 첫 화면 오른쪽 위 톱니바퀴: 소리, 진동, 어두운 화면, 큰 글씨, 누르기로 잇기
- 첫 화면 아래 칭호·점수 버튼: 점수 가게(블록 친구 꾸미기, 힌트·섞기, 테마 미리 열기). 가격은 `index.html`의 `ACC`, `TOOL_PRICE`, `THEME_PRICE`, 칭호는 `TITLES`
- 틀리게 이으면 기회가 1 줄어요(`miss` 함수). 단계별 기회 수는 `levelCfg` 안의 `c.moves`
- 숫자 잠금(200단계부터, 300단계부터 이중 잠금): `levelCfg`의 `lockP`(잠금 확률), `lock2`(이중 잠금 비율). 풀리는 규칙은 `unlockAround`
- 블록이 떨어진 뒤 답이 없으면 새 블록 숫자를 다시 골라요(`refillSmart`). 그래도 막히면 "판 섞기" 창이 떠요(`checkDead`)
- 태블릿(가장 짧은 변 600dp 이상)은 가로·세로 모두 돼요. 휴대폰은 세로 고정이에요(`MainActivity`의 `setRequestedOrientation`). 화면 배치는 `redesign.css` 맨 아래 "tablets & landscape" 부분

## Android 16(API 36) 대응
- `compileSdk`/`targetSdk` 36, Android Gradle Plugin 8.11.1. 처음 열 때 Android Studio가 Gradle이나 플러그인 업그레이드를 권하면 **업그레이드해도 괜찮아요.**
- 뒤로가기: Android 13 이상은 새 방식(`OnBackInvokedCallback`), 그 아래는 예전 방식으로 처리해요.
- Android 15 이상은 화면이 상태바 밑까지 깔려서, 시스템 바 높이만큼 여백을 줘요.
- 게임 파일은 `https://appassets.androidplatform.net/assets/`라는 앱 안 주소로 열어요(인터넷 연결은 하지 않아요). 결과 그림 만들기와 복사 기능 때문에 필요해요.

## 자랑하기 · 기록 옮기기 · 의견
- 결과 화면의 **카톡으로 자랑하기**: 결과 카드 그림을 만들어 카카오톡으로 바로 보내요. 카톡이 없으면 공유 목록이 떠요(`ShareProvider.java`, `MainActivity`의 `shareImage`).
- 설정 › **기록 옮기기**: 기록 전체를 `PANG1.`로 시작하는 코드로 복사하고, 새 폰에서 붙여 넣어 불러와요.
- 설정 › **의견 보내기**: 메일 앱을 열어 kimseohappiness@gmail.com 으로 보내요(`MainActivity`의 `sendMail`).
- 무한 모드는 오래 할수록 돌려받는 기회가 줄어서 언젠가 끝나요(`endlessBonus`).
