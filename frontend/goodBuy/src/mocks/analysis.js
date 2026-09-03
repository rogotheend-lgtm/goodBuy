const MOCK_TRANSACTIONS = [
  ['세븐일레븐 광주산정고려점', 16480, 'EXPENSE', 'FOOD', 'CONVENIENCE_STORE', false, 'NONE', null],
  ['세븐일레븐광주소촌이지점', 2750, 'EXPENSE', 'FOOD', 'CONVENIENCE_STORE', false, 'NONE', null],
  ['버거앤타코', 11600, 'EXPENSE', 'FOOD', 'FAST_FOOD', false, 'NONE', null],
  ['벌크커피하남소촌점', 3000, 'EXPENSE', 'FOOD', 'CAFE', false, 'NONE', null],
  ['맘스터치소촌점', 7900, 'EXPENSE', 'FOOD', 'FAST_FOOD', false, 'NONE', null],
  ['금호칼국수', 7500, 'EXPENSE', 'FOOD', 'RESTAURANT', false, 'NONE', null],
  [
    '토스페이_TOSS',
    630,
    'ANOMALY',
    'OTHER',
    'PAYMENT_GATEWAY',
    true,
    'AMBIGUOUS_PAYMENT_GATEWAY',
    '결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다.',
  ],
  [
    '토스페이_TOSS',
    620,
    'ANOMALY',
    'OTHER',
    'PAYMENT_GATEWAY',
    true,
    'AMBIGUOUS_PAYMENT_GATEWAY',
    '결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다.',
  ],
  ['다이소', 6000, 'EXPENSE', 'LIVING', 'HOUSEHOLD_STORE', false, 'NONE', null],
  ['389마트', 1200, 'EXPENSE', 'LIVING', 'MART', false, 'NONE', null],
]

/** 실제 OCR 대신 고정 예시 결과를 반환해 결과 화면을 개발할 수 있게 합니다. */
export async function createMockAnalysis({ images }) {
  // 실제 OCR을 호출할 때 보이는 로딩 화면을 mock에서도 충분히 확인할 수 있게 합니다.
  await delay(2000 + images.length * 350)

  const transactions = MOCK_TRANSACTIONS.map(
    ([
      counterparty,
      originalAmount,
      transactionType,
      purposeCategory,
      merchantType,
      anomaly,
      anomalyReason,
      anomalyDetail,
    ]) => ({
      counterparty,
      originalAmount,
      personalAmount: transactionType === 'EXPENSE' ? originalAmount : 0,
      transactionType,
      purposeCategory,
      merchantType,
      anomaly,
      anomalyReason,
      anomalyDetail,
    }),
  )

  const expenseTransactions = transactions.filter(
    (transaction) => transaction.transactionType === 'EXPENSE',
  )
  const anomalyTransactions = transactions.filter((transaction) => transaction.anomaly)
  const expenseAmount = sum(expenseTransactions, 'personalAmount')
  const dominantCategory = findDominantCategory(expenseTransactions, expenseAmount)

  return {
    transactions,
    categoryCatalogSource: 'MOCK',
    dominantCategory,
    summary: {
      parsedCount: transactions.length,
      parsedAmount: sum(transactions, 'originalAmount'),
      expenseCount: expenseTransactions.length,
      expenseAmount,
      selfTransferAmount: 0,
      otherPersonAmount: 0,
      anomalyCount: anomalyTransactions.length,
      anomalyAmount: sum(anomalyTransactions, 'originalAmount'),
    },
  }
}

function findDominantCategory(transactions, expenseAmount) {
  const totals = new Map()
  transactions.forEach((transaction) => {
    totals.set(
      transaction.purposeCategory,
      (totals.get(transaction.purposeCategory) || 0) + transaction.personalAmount,
    )
  })
  const [purposeCategory, amount] = Array.from(totals.entries()).sort(
    (left, right) => right[1] - left[1],
  )[0] || [null, 0]

  if (!purposeCategory) return null
  return {
    purposeCategory,
    amount,
    ratioPercent: expenseAmount ? Math.round((amount / expenseAmount) * 100) : 0,
    gifUrl: '/giphy.gif',
  }
}

function sum(transactions, field) {
  return transactions.reduce((total, transaction) => total + transaction[field], 0)
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}
