<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const userName = route.query.name || '회원'
</script>

<template>
  <div class="hero">
    <div class="hero-inner">
      <div class="eyebrow">
        AI 소비 분석 <span class="tag-done">분석 완료</span>
      </div>
      <h1>{{ userName }}님의 이번 달 소비 리포트</h1>
      <p class="sub">캡처를 바탕으로 분석한 결과예요</p>
    </div>
  </div>

  <div class="body">
    <div class="body-inner">
      <div class="card">
        <div class="label">AI 분석 리포트</div>
        <div class="placeholder">
          [ 결과 텍스트 영역 ]<br />AI 분석 결과 문구가 여기 표시됩니다<br />
          <span class="muted">— 백엔드 담당 개발 예정</span>
        </div>
      </div>
      <div class="card">
        <div class="label">소비 트렌드 비주얼</div>
        <div class="placeholder tall">
          [ 결과 GIF / 이미지 영역 ]<br /><span class="muted">klipy.com 연동 예정</span>
        </div>
      </div>
      <router-link to="/" class="cta-outline">다시 하기</router-link>
    </div>
  </div>
</template>

<style scoped>
.hero { background: #f7f8fa; padding: 52px 24px 36px; display: flex; justify-content: center; }
.hero-inner { width: 640px; }
.eyebrow { font-size: 12px; font-weight: 700; color: #2f5cff; margin-bottom: 8px; display: flex; gap: 10px; align-items: center; }
.tag-done { font-size: 11px; font-weight: 700; color: #0f9d58; background: #e6f7ec; border-radius: 20px; padding: 3px 10px; }
h1 { font-size: 28px; color: #111827; margin: 0 0 8px; }
.sub { font-size: 14px; color: #6b7280; margin: 0; }

.body { display: flex; justify-content: center; padding: 36px 24px; }
.body-inner { width: 640px; display: flex; flex-direction: column; gap: 20px; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 24px; }
.label { font-size: 14px; font-weight: 700; color: #111827; margin-bottom: 12px; }
.placeholder {
  border: 1.5px dashed #c7d2fe; border-radius: 10px; background: #f5f7ff;
  min-height: 130px; display: flex; align-items: center; justify-content: center;
  padding: 20px; text-align: center; font-size: 13px; color: #6b7280; line-height: 1.7;
}
.placeholder.tall { min-height: 220px; }
.muted { color: #9ca3af; }
.cta-outline {
  width: 140px; height: 46px; border-radius: 8px; border: 1px solid #e5e7eb;
  background: #fff; color: #111827; font-size: 14px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; text-decoration: none;
}
</style>