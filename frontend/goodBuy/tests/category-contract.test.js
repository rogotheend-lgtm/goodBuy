import assert from 'node:assert/strict'
import { before, test } from 'node:test'
import { createMockAnalysis } from '../src/mocks/analysis.js'

const allowedCategories = new Set([
  'FOOD', 'TRANSPORT', 'LIVING', 'SHOPPING', 'CULTURE_HOBBY',
  'HEALTH', 'EDUCATION', 'FIXED_SUBSCRIPTION', 'OTHER',
])
let result

before(async () => {
  result = await createMockAnalysis({ images: [{}] })
})

test('mock transactions keep field positions and the nine-category contract', () => {
  for (const transaction of result.transactions) {
    assert.equal(Object.hasOwn(transaction, 'merchantType'), false)
    assert.equal(allowedCategories.has(transaction.purposeCategory), true)
    assert.equal(typeof transaction.anomaly, 'boolean')
    assert.equal(typeof transaction.anomalyReason, 'string')
    assert.equal(typeof transaction.personalAmount, 'number')
    if (transaction.anomaly) {
      assert.equal(transaction.anomalyReason, 'AMBIGUOUS_PAYMENT_GATEWAY')
      assert.equal(transaction.personalAmount, 0)
      assert.match(transaction.anomalyDetail, /결제와 송금을 구분/)
    } else {
      assert.equal(transaction.anomalyReason, 'NONE')
      assert.equal(transaction.anomalyDetail, null)
      assert.equal(transaction.personalAmount, transaction.originalAmount)
    }
  }
})

test('mock summary and representative GIF remain unchanged', () => {
  assert.deepEqual(result.summary, {
    parsedCount: 10,
    parsedAmount: 57680,
    expenseCount: 8,
    expenseAmount: 56430,
    selfTransferAmount: 0,
    otherPersonAmount: 0,
    anomalyCount: 2,
    anomalyAmount: 1250,
  })
  assert.deepEqual(result.dominantCategory, {
    purposeCategory: 'FOOD',
    amount: 49230,
    ratioPercent: 87,
    gifUrl: '/giphy.gif',
  })
})
