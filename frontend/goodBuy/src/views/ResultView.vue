<script setup>
import { computed, ref } from 'vue'
import { useAnalysisStore } from '@/stores/analysis'

const { analysis, ownerName, clearResult } = useAnalysisStore()
const gifAvailable = ref(true)

const categoryLabels = {
  FOOD: '식비',
  TRANSPORT: '교통',
  LIVING: '생활',
  SHOPPING: '쇼핑',
  CULTURE_HOBBY: '문화·취미',
  HEALTH: '건강',
  EDUCATION: '교육',
  FIXED_SUBSCRIPTION: '고정·구독',
  OTHER: '기타',
}

const categorySummaries = computed(() => {
  if (!analysis.value) return []

  const totals = new Map()
  analysis.value.transactions
    .filter((transaction) => transaction.transactionType === 'EXPENSE')
    .forEach((transaction) => {
      const amount = totals.get(transaction.purposeCategory) || 0
      totals.set(transaction.purposeCategory, amount + transaction.personalAmount)
    })

  return Array.from(totals, ([category, amount]) => ({ category, amount })).sort(
    (left, right) => right.amount - left.amount,
  )
})

const reportSummary = computed(() => {
  if (!analysis.value) return ''

  const summary = analysis.value.summary
  const topCategory = categorySummaries.value[0]
  const anomalyText = summary.anomalyCount
    ? `결제·송금 구분이 필요한 이상치 ${summary.anomalyCount}건(${formatWon(summary.anomalyAmount)})도 함께 표시했어요.`
    : '별도로 확인할 이상치는 없어요.'

  if (!topCategory) {
    return `이번 분석에서 확정된 소비가 아직 없어요. ${anomalyText}`
  }

  const categoryName = categoryLabels[topCategory.category] || topCategory.category
  const ratio = summary.expenseAmount
    ? Math.round((topCategory.amount / summary.expenseAmount) * 100)
    : 0

  return `이번 달 확정 소비 금액은 ${formatWon(summary.expenseAmount)}이에요. 그중 ${categoryName}가 ${formatWon(topCategory.amount)}(${ratio}%)로 가장 큰 비중을 차지했고, ${anomalyText}`
})

const trendCaption = computed(() => {
  const topCategory = categorySummaries.value[0]
  if (!topCategory) return '소비 트렌드를 확인해보세요!'

  const categoryName = categoryLabels[topCategory.category] || topCategory.category
  return `${categoryName} 지출이 이번 달 1위예요!`
})

function formatWon(amount) {
  return `${Number(amount || 0).toLocaleString('ko-KR')}원`
}
</script>

<template>
  <template v-if="analysis">
    <div class="hero">
      <div class="hero-inner">
        <div class="eyebrow">
          AI 소비 분석 <span class="tag-done">분석 완료</span>
        </div>
        <h1>{{ ownerName }}님의 이번 달 소비 리포트</h1>
        <p class="sub">업로드한 캡처를 바탕으로 분석한 결과예요</p>
      </div>
    </div>

    <main class="body">
      <div class="result-layout">
        <div class="report-column">
          <section class="card report-card">
            <div class="label label-with-icon">
              <span class="label-icon document-icon" aria-hidden="true"></span>
              AI 분석 리포트
            </div>

            <div class="summary-grid">
              <article class="summary-card">
                <span>확정 소비</span>
                <strong>{{ formatWon(analysis.summary.expenseAmount) }}</strong>
                <small>{{ analysis.summary.expenseCount }}건</small>
              </article>
              <article class="summary-card">
                <span>전체 인식 금액</span>
                <strong>{{ formatWon(analysis.summary.parsedAmount) }}</strong>
                <small>{{ analysis.summary.parsedCount }}건</small>
              </article>
              <article class="summary-card warning">
                <span>이상치</span>
                <strong>{{ formatWon(analysis.summary.anomalyAmount) }}</strong>
                <small>{{ analysis.summary.anomalyCount }}건</small>
              </article>
            </div>

            <p class="report-copy">{{ reportSummary }}</p>
          </section>

          <section class="card trend-card">
            <div class="label label-with-icon">
              <span class="trend-icon" aria-hidden="true">↗</span>
              소비 트렌드 비주얼
            </div>
            <div class="gif-panel">
              <img
                v-if="gifAvailable"
                src="/giphy.gif"
                alt="수박을 발견한 커비 애니메이션"
                @error="gifAvailable = false"
              />
              <div v-else class="gif-fallback" aria-label="소비 분석 반응 이미지">💸</div>
              <strong>{{ trendCaption }}</strong>
            </div>
          </section>

          <router-link to="/" class="retry-button" @click="clearResult">다시 하기</router-link>
        </div>

        <aside class="chat-card" aria-label="AI 질문 기능 디자인 예시">
          <div class="chat-heading">
            <div class="label label-with-icon chat-title">
              <span class="bulb-icon" aria-hidden="true"></span>
              AI에게 물어보기
            </div>
            <p>소비 습관 관련 질문을 해보세요</p>
          </div>

          <div class="chat-box">
            <div class="bubble ai">
              안녕하세요! 이번 달 소비 내역에 대해 궁금한 점을 물어보세요.
            </div>
            <div class="bubble user">이번 달 옷 쇼핑에 얼마 썼어?</div>
            <div class="bubble ai">이번 달 쇼핑 카테고리에는 총 78,000원을 썼어요.</div>
            <div class="bubble user">그럼 식비는 얼마나 썼어?</div>
            <div class="bubble ai">식비는 218,000원으로 이번 달 지출 중 가장 큰 비중을 차지했어요.</div>
          </div>

          <div class="chat-input-row">
            <input
              class="chat-input"
              aria-label="질문 입력 디자인 예시"
              placeholder="질문을 입력하세요"
              readonly
            />
            <button class="chat-send" type="button" aria-label="전송 디자인 예시" disabled>→</button>
          </div>
        </aside>
      </div>
    </main>
  </template>

  <main v-else class="empty-state">
    <div class="card empty-card">
      <h1>표시할 분석 결과가 없어요</h1>
      <p>이미지를 업로드하고 분석을 먼저 진행해주세요.</p>
      <router-link to="/" class="retry-button">업로드 화면으로</router-link>
    </div>
  </main>
</template>

<style scoped>
.hero {
  display: flex;
  justify-content: center;
  padding: 42px 24px 30px;
  background: #f7f8fa;
}

.hero-inner,
.result-layout {
  width: min(760px, 100%);
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 9px;
  color: #2f5cff;
  font-size: 11px;
  font-weight: 800;
}

.tag-done {
  padding: 3px 9px;
  border-radius: 20px;
  background: #e6f7ec;
  color: #0f9d58;
  font-size: 9px;
  font-weight: 800;
}

h1 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 25px;
  letter-spacing: -0.04em;
}

.sub {
  margin: 0;
  color: #8a93a3;
  font-size: 12px;
}

.body {
  display: flex;
  justify-content: center;
  padding: 24px 24px 60px;
}

.result-layout {
  display: grid;
  grid-template-columns: minmax(0, 2.12fr) minmax(230px, 1fr);
  gap: 18px;
  align-items: start;
}

.report-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 16px;
}

.card,
.chat-card {
  border: 1px solid #e3e7ee;
  border-radius: 11px;
  background: #fff;
}

.card {
  padding: 20px;
}

.label {
  color: #202938;
  font-size: 12px;
  font-weight: 800;
}

.label-with-icon {
  display: flex;
  align-items: center;
  gap: 7px;
}

.label-icon {
  width: 12px;
  height: 14px;
  border: 1.7px solid #4c75ff;
  border-radius: 2px;
}

.document-icon::after {
  display: block;
  width: 5px;
  height: 1.5px;
  margin: 4px auto 0;
  background: #4c75ff;
  box-shadow: 0 3px 0 #4c75ff;
  content: '';
}

.trend-icon {
  color: #4c75ff;
  font-size: 16px;
  font-weight: 900;
}

.bulb-icon {
  position: relative;
  width: 11px;
  height: 11px;
  border: 1.5px solid #4c75ff;
  border-radius: 50%;
}

.bulb-icon::after {
  position: absolute;
  bottom: -4px;
  left: 2px;
  width: 5px;
  height: 3px;
  border-top: 1.5px solid #4c75ff;
  border-bottom: 1.5px solid #4c75ff;
  content: '';
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 15px;
}

.summary-card {
  min-width: 0;
  padding: 14px 12px;
  border-radius: 8px;
  background: #f7f8fa;
}

.summary-card.warning {
  background: #fff7eb;
}

.summary-card span,
.summary-card small {
  display: block;
  color: #8a93a3;
  font-size: 9px;
}

.summary-card strong {
  display: block;
  margin: 5px 0 3px;
  overflow-wrap: anywhere;
  color: #202938;
  font-size: 16px;
}

.summary-card.warning strong {
  color: #e4652d;
}

.report-copy {
  margin: 18px 0 0;
  color: #4b5563;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.9;
}

.trend-card {
  padding-bottom: 16px;
}

.gif-panel {
  display: flex;
  min-height: 160px;
  margin-top: 13px;
  padding: 20px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 8px;
  background: #f4f6ff;
}

.gif-panel img,
.gif-fallback {
  width: 110px;
  height: 82px;
  border-radius: 8px;
}

.gif-panel img {
  display: block;
  object-fit: cover;
}

.gif-panel strong {
  margin-top: 9px;
  color: #202938;
  font-size: 11px;
}

.gif-fallback {
  display: grid;
  place-items: center;
  background: #fff;
  font-size: 38px;
}

.retry-button {
  display: flex;
  width: 102px;
  height: 40px;
  align-items: center;
  justify-content: center;
  border: 1px solid #e3e7ee;
  border-radius: 7px;
  background: #fff;
  color: #202938;
  font-size: 12px;
  font-weight: 700;
  text-decoration: none;
}

.chat-card {
  display: flex;
  min-height: 598px;
  overflow: hidden;
  flex-direction: column;
}

.chat-heading {
  padding: 14px 14px 12px;
  border-bottom: 1px solid #edf0f4;
}

.chat-title {
  gap: 5px;
}

.chat-heading p {
  margin: 2px 0 0 21px;
  color: #a0a7b3;
  font-size: 9px;
}

.chat-box {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  padding: 16px 13px;
}

.bubble {
  max-width: 84%;
  padding: 9px 11px;
  border-radius: 9px;
  font-size: 10px;
  line-height: 1.6;
}

.bubble.ai {
  align-self: flex-start;
  border-radius: 9px 9px 9px 3px;
  background: #f3f4f6;
  color: #596273;
}

.bubble.user {
  align-self: flex-end;
  border-radius: 9px 9px 3px 9px;
  background: #2f5cff;
  color: #fff;
}

.chat-input-row {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #edf0f4;
}

.chat-input {
  min-width: 0;
  height: 34px;
  flex: 1;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  outline: none;
  background: #fff;
  color: #9ca3af;
  font-size: 10px;
}

.chat-input::placeholder {
  color: #b5bac3;
}

.chat-send {
  display: grid;
  width: 34px;
  height: 34px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: #2f5cff;
  color: #fff;
  font-size: 18px;
}

.empty-state {
  display: grid;
  min-height: calc(100vh - 68px);
  padding: 24px;
  place-items: center;
  background: #f7f8fa;
}

.empty-card {
  width: min(520px, 100%);
  text-align: center;
}

.empty-card p {
  color: #6b7280;
}

.empty-card .retry-button {
  width: 150px;
  margin: 20px auto 0;
}

@media (max-width: 760px) {
  .result-layout {
    grid-template-columns: 1fr;
  }

  .chat-card {
    min-height: 480px;
  }
}

@media (max-width: 520px) {
  .hero {
    padding: 32px 18px 25px;
  }

  h1 {
    font-size: 22px;
  }

  .body {
    padding: 18px 16px 40px;
  }

  .card {
    padding: 16px;
  }

  .summary-grid {
    gap: 7px;
  }

  .summary-card {
    padding: 12px 8px;
  }

  .summary-card strong {
    font-size: 13px;
  }

  .gif-panel {
    min-height: 150px;
  }

  .chat-card {
    min-height: 450px;
  }
}
</style>
