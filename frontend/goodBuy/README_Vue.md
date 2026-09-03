# goodBuy

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

기본값은 백엔드 없이 업로드와 결과 화면을 확인하는 mock 모드입니다.

실제 Spring `POST /api/v1/analyses`를 호출하려면 Spring을 8080 포트에서 실행한 뒤 다음처럼 시작합니다.

```sh
VITE_ANALYSIS_MODE=backend npm run dev
```

개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다. 별도 배포 주소를 사용할 때는
`VITE_API_BASE_URL`도 지정할 수 있습니다.

## 실제 구현과 Mock 구분

| 기능 | 현재 상태 | 설명 |
|---|---|---|
| 이름·이미지 입력 검증 | 실제 구현 | 이름 2~30자, PNG/JPEG, 최대 5장, 파일당 10MB를 검사합니다. |
| 업로드·결과 화면 이동 | 실제 구현 | 분석 요청 중 로딩을 표시하고 완료되면 결과 화면으로 이동합니다. |
| 거래·합계·이상치 출력 | 실제 구현 | Spring 응답의 `transactions`, `summary` 필드를 화면에 표시합니다. |
| 카테고리별 금액 계산 | 실제 구현 | 확정 소비 거래를 카테고리별로 프론트에서 합산합니다. |
| Spring API 호출 | 실제 구현 | `VITE_ANALYSIS_MODE=backend`에서 `POST /api/v1/analyses`를 호출합니다. |
| 기본 OCR 분석 결과 | Mock | 기본 실행에서는 업로드 이미지 내용을 읽지 않고 고정 예시 거래를 반환합니다. |
| 실제 OCR | 선택 가능 | Spring을 `OCR_MODE=python`으로 실행하면 Python PaddleOCR 결과를 사용합니다. |
| AI 요약 문구 | Mock | 기존 합계·카테고리 필드로 프론트에서 예시 문장을 생성합니다. |
| 소비 반응 GIF | Mock | 현재는 고정 예시 GIF이며 추후 GIF API 결과로 교체할 수 있습니다. |
| 결과 기록·재조회 | 미구현 | 로그인, 브라우저 저장, 결과 조회 API 없이 현재 화면 메모리에만 보관합니다. |

### Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
