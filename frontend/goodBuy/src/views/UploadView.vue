<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 화면 상태값들 (자바로 치면 필드에 해당, Vue에서는 ref()로 감싸면 값이 바뀔 때 화면이 자동으로 다시 그려져)
const name = ref('')
const files = ref([])         // 업로드한 파일들 (최대 3개)
const status = ref('idle')    // 'idle' = 기본 상태, 'analyzing' = 분석 중
const progress = ref(0)
const steps = ref([])         // 캡처별 분석 상태 목록
const categories = ref([])    // 발견된 소비 카테고리

// computed: name/files 값이 바뀔 때마다 자동으로 다시 계산되는 값
const canSubmit = computed(
  () => name.value.trim() !== '' && files.value.length > 0 && status.value === 'idle'
)

function onFileChange(e) {
  files.value = Array.from(e.target.files).slice(0, 3)
}

function startAnalysis() {
  if (!canSubmit.value) return
  status.value = 'analyzing'
  progress.value = 0
  categories.value = []
  steps.value = files.value.map((f, i) => ({
    label: `캡처 ${i + 1} 분석`,
    state: i === 0 ? 'analyzing' : 'pending',
  }))

  // ⚠️ TODO (백엔드 연동 자리):
  // 1) FormData에 name, files 담아서 Spring Boot에 POST 요청
  // 2) 응답으로 세션ID 받으면 new EventSource(...)로 SSE 연결
  // 3) SSE 메시지 올 때마다 steps / progress / categories 업데이트
  // 지금은 백엔드가 없으니 setTimeout으로 흉내만 냄 (아래 함수)
  simulateProgress()
}

function simulateProgress() {
  let step = 0
  const timer = setInterval(() => {
    step++
    if (step <= steps.value.length) {
      steps.value[step - 1].state = 'done'
      if (step < steps.value.length) steps.value[step].state = 'analyzing'
      progress.value = Math.round((step / steps.value.length) * 100)
      if (step === 1) categories.value.push('카페')
      if (step === 2) categories.value.push('쇼핑')
    }
    if (step >= steps.value.length) {
      clearInterval(timer)
      setTimeout(() => router.push({ name: 'result', query: { name: name.value } }), 500)
    }
  }, 1200)
}
</script>

<template>
  <div class="hero">
    <div class="hero-inner">
      <div class="eyebrow">AI 소비 분석</div>

      <template v-if="status === 'idle'">
        <h1>이번 달 소비내역, 확인해볼까요?</h1>
        <p class="sub">카드 앱에서 소비내역 화면을 캡처해서 올리면 AI가 자동으로 분석해요</p>
        <ul class="checklist">
          <li>캡처 이미지만 올리면 자동으로 항목을 분류해요</li>
          <li>실시간으로 분석 진행 상황을 확인할 수 있어요</li>
          <li>분석 결과와 함께 어울리는 반응 GIF를 보여줘요</li>
        </ul>
      </template>
      <template v-else>
        <h1>업로드한 내용을 분석하고 있어요</h1>
        <p class="sub">화면을 벗어나도 괜찮아요, 완료되면 바로 알려드릴게요</p>
      </template>
    </div>
  </div>

  <div class="body">
    <div class="body-inner">

      <template v-if="status === 'idle'">
        <div class="card">
          <div class="label">이름 (본인 확인용)</div>
          <div class="hint">내 통장 간 단순 이체는 소비 내역에서 제외돼요</div>
          <input v-model="name" type="text" placeholder="이름을 입력해주세요" class="input" />
        </div>

        <div class="card">
          <div class="label">소비내역 캡처 업로드</div>
          <div class="hint">카드 앱 소비내역 화면을 최대 3장까지 올려주세요</div>
          <label class="dropzone">
            <input type="file" accept="image/*" multiple @change="onFileChange" hidden />
            업로드된 파일: {{ files.length }}장
          </label>
        </div>

        <button class="cta" :disabled="!canSubmit" @click="startAnalysis">
          분석 시작하기
        </button>
      </template>

      <template v-else>
        <div class="card">
          <div class="progress-header">
            <span class="label">분석 진행 상황</span>
            <span class="badge">SSE 실시간 업데이트</span>
          </div>
          <div class="bar"><div class="bar-fill" :style="{ width: progress + '%' }"></div></div>

          <div v-for="(s, i) in steps" :key="i" class="step-row">
            <span>{{ s.label }}</span>
            <span v-if="s.state === 'done'" class="done">완료</span>
            <span v-else-if="s.state === 'analyzing'" class="analyzing">진행 중</span>
            <span v-else class="pending">대기 중</span>
          </div>

          <div v-if="categories.length" class="chips">
            <span v-for="c in categories" :key="c" class="chip">{{ c }}</span>
          </div>
        </div>

        <button class="cta" disabled>분석 중...</button>
      </template>

    </div>
  </div>
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
.dropzone {
  display: inline-flex; align-items: center; justify-content: center;
  width: 200px; height: 60px; border: 1.5px dashed #c7d2fe; border-radius: 10px;
  background: #f5f7ff; color: #2f5cff; font-size: 13px; cursor: pointer;
}
.cta {
  width: 190px; height: 48px; border: none; border-radius: 8px;
  background: #2f5cff; color: #fff; font-weight: 700; cursor: pointer;
}
.cta:disabled { background: #e5e7eb; color: #9ca3af; cursor: not-allowed; }

.progress-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.badge { font-size: 11px; font-weight: 700; color: #2f5cff; background: #eef2ff; border-radius: 20px; padding: 4px 10px; }
.bar { height: 8px; border-radius: 4px; background: #f0f1f3; overflow: hidden; margin-bottom: 12px; }
.bar-fill { height: 100%; background: #2f5cff; transition: width .4s; }
.step-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f3f4f6; font-size: 13px; }
.done { color: #2f5cff; font-weight: 600; }
.analyzing { color: #2f5cff; font-weight: 600; }
.pending { color: #9ca3af; }
.chips { display: flex; gap: 8px; margin-top: 12px; }
.chip { background: #eef2ff; color: #2f5cff; font-size: 12px; font-weight: 700; padding: 6px 12px; border-radius: 20px; }
</style>