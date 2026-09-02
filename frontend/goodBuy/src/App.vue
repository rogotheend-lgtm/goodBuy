<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="navbar">
    <div class="brand">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <rect x="3" y="12" width="4" height="9" rx="1" fill="#2f5cff" />
        <rect x="10" y="7" width="4" height="14" rx="1" fill="#2f5cff" />
        <rect x="17" y="3" width="4" height="18" rx="1" fill="#2f5cff" />
      </svg>
      <span class="brand-name">소비분석 AI</span>
      <span class="tag">PoC</span>
    </div>
    <nav>
      <RouterLink to="/" active-class="active">업로드</RouterLink>
      <RouterLink to="/result" active-class="active">결과 보기</RouterLink>
    </nav>
  </header>

  <RouterView />
</template>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, "Apple SD Gothic Neo", sans-serif; }

.navbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 48px; height: 68px; border-bottom: 1px solid #e5e7eb;
}
.brand { display: flex; align-items: center; gap: 9px; }
.brand-name { font-size: 16px; font-weight: 800; color: #111827; }
.tag {
  font-size: 11px; font-weight: 700; color: #6b7280;
  background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 20px; padding: 3px 10px;
}
nav { display: flex; gap: 28px; }
nav a { font-size: 13px; font-weight: 500; color: #9ca3af; text-decoration: none; }
nav a.active { font-weight: 700; color: #2f5cff; }
</style>