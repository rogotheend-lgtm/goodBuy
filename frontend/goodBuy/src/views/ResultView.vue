<script setup>
import { computed, ref } from 'vue'
import { useAnalysisStore } from '@/stores/analysis'

const { analysis, ownerName, sourceMode, clearResult } = useAnalysisStore()
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

const transactionTypeLabels = {
  EXPENSE: '소비',
  SELF_TRANSFER: '본인 이체',
  OTHER_PERSON: '다른 사람 거래',
  ANOMALY: '이상치',
}

const anomalyLabels = {
  SELF_TRANSFER: '본인 계좌 이체',
  AMBIGUOUS_PAYMENT_GATEWAY: '결제·송금 구분 필요',
  GROUP_PAYMENT_CANDIDATE: '단체 결제 가능성',
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

const mockAiSummary = computed(() => {
  if (!analysis.value) return ''

  const topCategory = categorySummaries.value[0]
  const anomalyText = analysis.value.summary.anomalyCount
    ? `확인이 필요한 이상치 ${analysis.value.summary.anomalyCount}건도 발견했어요.`
    : '별도로 확인할 이상치는 없어요.'

  if (!topCategory) {
    return `${ownerName.value}님은 이번 분석에서 확정된 소비가 아직 없어요. ${anomalyText}`
  }

  const categoryName = categoryLabels[topCategory.category] || topCategory.category
  return `${ownerName.value}님은 이번 분석에서 ${categoryName}에 ${formatWon(topCategory.amount)}을 사용했어요. ${anomalyText}`
})

const mockGifUrl = 'https://media.giphy.com/media/l0Ex6kAKAoFRsFh6M/giphy.gif'

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
          <span v-if="sourceMode === 'mock'" class="tag-mock">예시 데이터</span>
        </div>
        <h1>{{ ownerName }}님의 소비 리포트</h1>
        <p class="sub">업로드한 캡처를 바탕으로 구성한 결과예요</p>
      </div>
    </div>

    <main class="body">
      <div class="body-inner">
        <section class="summary-grid">
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
        </section>

        <section class="card">
          <div class="label">AI 요약 문구 <span class="mock-label">MOCK</span></div>
          <p class="ai-copy">{{ mockAiSummary }}</p>
        </section>

        <section class="card">
          <div class="label">소비 트렌드 반응 GIF <span class="mock-label">MOCK</span></div>
          <div class="gif-box">
            <img
              v-if="gifAvailable"
              :src="mockGifUrl"
              alt="소비 분석 예시 반응 GIF"
              @error="gifAvailable = false"
            />
            <div v-else class="gif-fallback" aria-label="소비 분석 반응 예시">💸</div>
            <p>현재는 화면 확인용 예시이며 추후 GIF API 결과로 교체할 수 있어요.</p>
          </div>
        </section>

        <section class="card">
          <div class="label">카테고리별 사용 금액</div>
          <div class="category-list">
            <div v-for="item in categorySummaries" :key="item.category" class="category-row">
              <span>{{ categoryLabels[item.category] || item.category }}</span>
              <strong>{{ formatWon(item.amount) }}</strong>
            </div>
            <p v-if="!categorySummaries.length" class="empty-copy">확정된 소비 내역이 없습니다.</p>
          </div>
        </section>

        <section class="card">
          <div class="label">거래 내역</div>
          <div class="transaction-list">
            <article
              v-for="(transaction, index) in analysis.transactions"
              :key="`${transaction.counterparty}-${index}`"
              class="transaction"
              :class="{ anomaly: transaction.anomaly }"
            >
              <div>
                <strong>{{ transaction.counterparty }}</strong>
                <span>{{ categoryLabels[transaction.purposeCategory] || transaction.purposeCategory }}</span>
              </div>
              <div class="transaction-right">
                <strong>{{ formatWon(transaction.originalAmount) }}</strong>
                <span>{{ transactionTypeLabels[transaction.transactionType] || transaction.transactionType }}</span>
              </div>
              <p v-if="transaction.anomaly" class="anomaly-detail">
                {{ anomalyLabels[transaction.anomalyReason] || transaction.anomalyReason }} ·
                {{ transaction.anomalyDetail }}
              </p>
            </article>
            <p v-if="!analysis.transactions.length" class="empty-copy">인식된 거래 내역이 없습니다.</p>
          </div>
        </section>

        <router-link to="/" class="cta-outline" @click="clearResult">다시 분석하기</router-link>
      </div>
    </main>
  </template>

  <main v-else class="empty-state">
    <div class="card empty-card">
      <h1>표시할 분석 결과가 없어요</h1>
      <p>이미지를 업로드하고 분석을 먼저 진행해주세요.</p>
      <router-link to="/" class="cta-outline">업로드 화면으로</router-link>
    </div>
  </main>
</template>

<style scoped>
.hero { background: #f7f8fa; padding: 52px 24px 36px; display: flex; justify-content: center; }
.hero-inner, .body-inner { width: min(760px, 100%); }
.eyebrow { display: flex; align-items: center; flex-wrap: wrap; gap: 9px; margin-bottom: 8px; color: #2f5cff; font-size: 12px; font-weight: 700; }
.tag-done, .tag-mock, .mock-label { border-radius: 20px; padding: 3px 9px; font-size: 10px; font-weight: 800; }
.tag-done { background: #e6f7ec; color: #0f9d58; }
.tag-mock, .mock-label { background: #fff4d6; color: #a16207; }
h1 { margin: 0 0 8px; color: #111827; font-size: 28px; }
.sub { margin: 0; color: #6b7280; font-size: 14px; }
.body { display: flex; justify-content: center; padding: 36px 24px 60px; }
.body-inner { display: flex; flex-direction: column; gap: 20px; }
.card, .summary-card { border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; padding: 24px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.summary-card span, .summary-card small { display: block; color: #6b7280; font-size: 12px; }
.summary-card strong { display: block; margin: 8px 0 5px; color: #111827; font-size: 22px; }
.summary-card.warning strong { color: #dc6b21; }
.label { margin-bottom: 14px; color: #111827; font-size: 14px; font-weight: 800; }
.mock-label { margin-left: 6px; }
.ai-copy { margin: 0; color: #374151; font-size: 15px; line-height: 1.8; }
.gif-box { text-align: center; }
.gif-box img { width: 100%; max-width: 360px; height: 220px; border-radius: 12px; object-fit: cover; }
.gif-box p { margin: 10px 0 0; color: #9ca3af; font-size: 12px; }
.gif-fallback { display: grid; place-items: center; width: 100%; max-width: 360px; height: 220px; margin: 0 auto; border-radius: 12px; background: #f5f7ff; font-size: 72px; animation: float 1.1s ease-in-out infinite alternate; }
@keyframes float { to { transform: translateY(-8px); } }
.category-list, .transaction-list { display: flex; flex-direction: column; }
.empty-copy { margin: 0; color: #9ca3af; font-size: 13px; }
.category-row { display: flex; justify-content: space-between; padding: 13px 0; border-bottom: 1px solid #f0f1f3; color: #374151; font-size: 13px; }
.category-row:last-child { border-bottom: 0; }
.transaction { display: grid; grid-template-columns: 1fr auto; gap: 8px 16px; padding: 16px 0; border-bottom: 1px solid #f0f1f3; }
.transaction:last-child { border-bottom: 0; }
.transaction > div { display: flex; flex-direction: column; gap: 5px; }
.transaction span { color: #9ca3af; font-size: 11px; }
.transaction-right { text-align: right; }
.transaction.anomaly { margin: 5px 0; padding: 16px; border: 1px solid #fed7aa; border-radius: 10px; background: #fffaf5; }
.anomaly-detail { grid-column: 1 / -1; margin: 4px 0 0; color: #9a3412; font-size: 12px; line-height: 1.6; }
.cta-outline { display: flex; align-items: center; justify-content: center; width: 150px; height: 46px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; color: #111827; font-size: 14px; font-weight: 700; text-decoration: none; }
.empty-state { display: grid; min-height: calc(100vh - 68px); place-items: center; padding: 24px; background: #f7f8fa; }
.empty-card { width: min(520px, 100%); text-align: center; }
.empty-card p { color: #6b7280; }
.empty-card .cta-outline { margin: 20px auto 0; }

@media (max-width: 640px) {
  .summary-grid { grid-template-columns: 1fr; }
  .hero { padding-top: 36px; }
  h1 { font-size: 24px; }
  .card, .summary-card { padding: 18px; }
}
</style>
