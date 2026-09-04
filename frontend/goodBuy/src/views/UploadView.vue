<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { analysisMode, analyzeExpenses } from '@/api/analysis'
import { useAnalysisStore } from '@/stores/analysis'

const router = useRouter()
const { saveResult } = useAnalysisStore()

const name = ref('')
const files = ref([])
const status = ref('idle')
const errorMessage = ref('')
const progress = ref(0)
const steps = ref([])
const discoveredCategories = ref([])
let progressTimer

const MAX_FILE_COUNT = 5
const MAX_FILE_SIZE = 10 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set(['image/png', 'image/jpeg'])
const MOCK_CATEGORY_SEQUENCE = [
  '식비',
  '교통',
  '생활',
  '쇼핑',
  '문화·취미',
  '건강',
  '교육',
  '고정·구독',
  '기타',
]

const normalizedNameLength = computed(() => name.value.replace(/\s/g, '').length)
const nameIsValid = computed(
  () => normalizedNameLength.value >= 2 && normalizedNameLength.value <= 30,
)
const canSubmit = computed(
  () => nameIsValid.value && files.value.length > 0 && status.value !== 'analyzing',
)

function onFileChange(event) {
  errorMessage.value = ''
  const selectedFiles = Array.from(event.target.files)

  if (selectedFiles.length > MAX_FILE_COUNT) {
    resetFileInput(event)
    errorMessage.value = '이미지는 최대 5장까지 선택할 수 있습니다.'
    return
  }

  const unsupportedFile = selectedFiles.find((file) => !ALLOWED_IMAGE_TYPES.has(file.type))
  if (unsupportedFile) {
    resetFileInput(event)
    errorMessage.value = `${unsupportedFile.name}: PNG 또는 JPEG 이미지만 업로드할 수 있습니다.`
    return
  }

  const oversizedFile = selectedFiles.find((file) => file.size > MAX_FILE_SIZE)
  if (oversizedFile) {
    resetFileInput(event)
    errorMessage.value = `${oversizedFile.name}: 이미지 한 장은 10MB를 넘을 수 없습니다.`
    return
  }

  files.value = selectedFiles
}

function resetFileInput(event) {
  files.value = []
  event.target.value = ''
}

async function startAnalysis() {
  if (!canSubmit.value) return

  status.value = 'analyzing'
  errorMessage.value = ''
  startProgress()

  try {
    const result = await analyzeExpenses({
      ownerName: name.value.trim(),
      images: files.value,
    })
    finishProgress()
    saveResult(result, name.value.trim(), analysisMode)
    await router.push({ name: 'result' })
  } catch (error) {
    stopProgress()
    status.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '분석 중 오류가 발생했습니다.'
  }
}

function startProgress() {
  progress.value = Math.max(12, Math.round(45 / files.value.length))
  discoveredCategories.value = analysisMode === 'mock' ? [MOCK_CATEGORY_SEQUENCE[0]] : []
  steps.value = files.value.map((_, index) => ({
    label: `캡처 ${index + 1} 분석`,
    state: index === 0 ? 'analyzing' : 'pending',
  }))

  const startedAt = window.performance.now()
  const estimatedStepMilliseconds = 3500
  const estimatedTotalMilliseconds = Math.max(5000, files.value.length * estimatedStepMilliseconds)

  progressTimer = window.setInterval(() => {
    const elapsed = window.performance.now() - startedAt
    const currentIndex = Math.min(
      steps.value.length - 1,
      Math.floor(elapsed / estimatedStepMilliseconds),
    )

    steps.value.forEach((step, index) => {
      step.state = index < currentIndex ? 'done' : index === currentIndex ? 'analyzing' : 'pending'
    })

    if (analysisMode === 'mock') {
      discoveredCategories.value = MOCK_CATEGORY_SEQUENCE.slice(0, currentIndex + 1)
    }

    const elapsedRatio = Math.min(1, elapsed / estimatedTotalMilliseconds)
    progress.value = Math.min(92, Math.round(12 + elapsedRatio * 80))
  }, 250)
}

function finishProgress() {
  stopProgress()
  steps.value.forEach((step) => {
    step.state = 'done'
  })
  progress.value = 100
}

function stopProgress() {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = undefined
  }
}

onBeforeUnmount(stopProgress)
</script>

<template>
  <div class="hero">
    <div class="hero-inner">
      <div v-if="status !== 'analyzing'" class="eyebrow">AI 소비 분석</div>

      <template v-if="status !== 'analyzing'">
        <h1>이번 달 소비내역, 확인해볼까요?</h1>
        <p class="sub">소비내역 화면을 캡처해서 올리면 거래와 이상치를 한 번에 확인할 수 있어요</p>
        <ul class="checklist">
          <li>캡처 이미지만 올리면 자동으로 항목을 분류해요</li>
          <li>실제 소비와 확인이 필요한 이상치를 나누어 보여줘요</li>
          <li v-if="analysisMode === 'mock'">현재 화면 개발 단계에서는 예시 분석 결과를 사용해요</li>
          <li v-else>Spring·Python OCR과 DB 카테고리 기준으로 실제 분석해요</li>
        </ul>
      </template>
      <template v-else>
        <div class="analyzing-title">
          <span class="search-icon" aria-hidden="true">⌕</span>
          <div>
            <div class="eyebrow">AI 소비 분석</div>
            <h1>업로드한 내용을 분석하고 있어요</h1>
          </div>
        </div>
      </template>
    </div>
  </div>

  <main class="body">
    <div class="body-inner">
      <template v-if="status !== 'analyzing'">
        <section class="card">
          <div class="label">이름 (본인 확인용)</div>
          <div class="hint">내 통장 간 단순 이체는 소비 내역에서 제외돼요</div>
          <input
            v-model="name"
            type="text"
            minlength="2"
            maxlength="30"
            placeholder="이름을 입력해주세요"
            class="input"
          />
          <div v-if="name && !nameIsValid" class="field-error">공백 제외 2~30자로 입력해주세요.</div>
        </section>

        <section class="card">
          <div class="label">소비내역 캡처 업로드</div>
          <div class="hint">PNG 또는 JPEG 소비내역 화면을 최대 5장까지 올려주세요</div>
          <label class="dropzone">
            <input
              type="file"
              accept="image/png,image/jpeg"
              multiple
              @change="onFileChange"
              hidden
            />
            업로드된 파일: {{ files.length }}장
          </label>
          <ul v-if="files.length" class="file-list">
            <li v-for="file in files" :key="`${file.name}-${file.size}`">{{ file.name }}</li>
          </ul>
        </section>

        <div v-if="errorMessage" class="error-box" role="alert">{{ errorMessage }}</div>

        <button class="cta" :disabled="!canSubmit" @click="startAnalysis">
          {{ status === 'error' ? '다시 분석하기' : '분석 시작하기' }}
        </button>
      </template>

      <template v-else>
        <section class="card progress-card">
          <div class="progress-header">
            <span class="label">분석 진행 상황</span>
            <span class="progress-badge">
              {{ analysisMode === 'mock' ? '예시 분석 중' : '실시간 분석 중' }}
            </span>
          </div>
          <div
            class="progress-track"
            role="progressbar"
            aria-label="분석 진행률"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-valuenow="progress"
          >
            <div
              class="progress-fill"
              :style="{ width: `${progress}%` }"
            ></div>
          </div>

          <div class="step-list">
            <div v-for="step in steps" :key="step.label" class="step-row">
              <span class="step-name">
                <span v-if="step.state === 'done'" class="step-icon done">✓</span>
                <span v-else-if="step.state === 'analyzing'" class="step-icon spinner"></span>
                <span v-else class="step-icon pending"></span>
                {{ step.label }}
              </span>
              <span v-if="step.state === 'done'" class="step-status done">완료</span>
              <span v-else-if="step.state === 'analyzing'" class="step-status analyzing">•••</span>
              <span v-else class="step-status waiting">대기 중</span>
            </div>
          </div>

          <div v-if="discoveredCategories.length" class="category-progress">
            <div class="category-progress-label">지금까지 발견된 소비 카테고리</div>
            <div class="category-chips">
              <span v-for="category in discoveredCategories" :key="category" class="category-chip">
                {{ category }}
              </span>
              <span class="category-chip pending-chip">분석 중</span>
            </div>
          </div>
        </section>

        <div class="analysis-tip">💡 잠깐, 어떤 소비 습관이 보일지 확인하고 있어요.</div>
        <button class="cta loading-button" disabled>
          <span class="button-spinner" aria-hidden="true"></span>
          분석 중...
        </button>
      </template>
    </div>
  </main>
</template>

<style scoped>
.hero { background: #f7f8fa; padding: 52px 24px 36px; display: flex; justify-content: center; }
.hero-inner { width: 640px; }
.eyebrow { font-size: 12px; font-weight: 700; color: #2f5cff; margin-bottom: 8px; }
h1 { font-size: 28px; color: #111827; margin: 0 0 8px; }
.sub { font-size: 14px; color: #6b7280; margin: 0; }
.checklist { list-style: none; padding: 0; margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
.checklist li { font-size: 13px; color: #374151; padding-left: 26px; position: relative; }
.checklist li::before {
  content: '✓'; position: absolute; left: 0; top: 0; width: 18px; height: 18px;
  background: #111827; color: #fff; border-radius: 50%; font-size: 11px;
  display: flex; align-items: center; justify-content: center;
}
.body { display: flex; justify-content: center; padding: 36px 24px; }
.body-inner { width: 640px; display: flex; flex-direction: column; gap: 20px; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 24px; }
.label { font-size: 13px; font-weight: 700; color: #111827; }
.hint { font-size: 12px; color: #9ca3af; margin: 6px 0 10px; }
.input { width: 300px; height: 44px; border: 1px solid #e5e7eb; border-radius: 8px; padding: 0 14px; }
.field-error { color: #dc2626; font-size: 12px; margin-top: 8px; }
.dropzone {
  display: inline-flex; align-items: center; justify-content: center;
  width: 200px; height: 60px; border: 1.5px dashed #c7d2fe; border-radius: 10px;
  background: #f5f7ff; color: #2f5cff; font-size: 13px; cursor: pointer;
}
.file-list { margin: 12px 0 0; padding-left: 20px; color: #6b7280; font-size: 12px; line-height: 1.8; }
.error-box { padding: 14px 16px; border: 1px solid #fecaca; border-radius: 10px; background: #fef2f2; color: #b91c1c; font-size: 13px; }
.cta {
  width: 190px; height: 48px; border: none; border-radius: 8px;
  background: #2f5cff; color: #fff; font-weight: 700; cursor: pointer;
}
.cta:disabled { background: #e5e7eb; color: #9ca3af; cursor: not-allowed; }
.analyzing-title { display: flex; align-items: center; gap: 16px; }
.analyzing-title .eyebrow { margin-bottom: 5px; }
.search-icon { display: grid; place-items: center; width: 54px; height: 54px; flex: 0 0 54px; border: 3px solid #e0e7ff; border-radius: 50%; color: #2f5cff; font-size: 29px; font-weight: 700; }
.progress-card { padding: 26px; }
.progress-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.progress-badge { padding: 5px 10px; border-radius: 20px; background: #eef2ff; color: #2f5cff; font-size: 10px; font-weight: 800; }
.progress-track { height: 8px; margin: 18px 0 10px; overflow: hidden; border-radius: 999px; background: #eef0f3; }
.progress-fill { height: 100%; border-radius: inherit; background: #4876ff; transition: width .45s ease; }
.step-list { display: flex; flex-direction: column; }
.step-row { display: flex; align-items: center; justify-content: space-between; min-height: 48px; border-bottom: 1px solid #f0f1f3; font-size: 13px; }
.step-row:last-child { border-bottom: 0; }
.step-name { display: flex; align-items: center; gap: 10px; color: #374151; }
.step-icon { display: inline-grid; place-items: center; width: 16px; height: 16px; }
.step-icon.done { color: #2f5cff; font-weight: 900; }
.step-icon.pending { width: 8px; height: 8px; margin: 4px; border-radius: 50%; background: #d1d5db; }
.step-icon.spinner { border: 2px solid #dbe3ff; border-top-color: #2f5cff; border-radius: 50%; animation: spin .8s linear infinite; }
.step-status { font-size: 11px; font-weight: 700; }
.step-status.done, .step-status.analyzing { color: #2f5cff; }
.step-status.waiting { color: #9ca3af; }
.category-progress { margin-top: 12px; padding-top: 16px; border-top: 1px solid #f0f1f3; }
.category-progress-label { margin-bottom: 10px; color: #9ca3af; font-size: 11px; }
.category-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.category-chip { padding: 6px 10px; border-radius: 999px; background: #eef2ff; color: #2f5cff; font-size: 11px; font-weight: 700; }
.category-chip.pending-chip { background: #f3f4f6; color: #9ca3af; }
.analysis-tip { padding: 14px 17px; border-radius: 10px; background: #eef2ff; color: #4b5563; font-size: 12px; }
.loading-button { display: inline-flex; align-items: center; justify-content: center; gap: 9px; }
.button-spinner { width: 14px; height: 14px; border: 2px solid #c9cdd5; border-top-color: #7c8491; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 680px) {
  .hero-inner, .body-inner { width: 100%; }
  .input, .cta { width: 100%; }
}
</style>
