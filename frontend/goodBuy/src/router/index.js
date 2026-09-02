import { createRouter, createWebHistory } from 'vue-router'
import UploadView from '../views/UploadView.vue'
import ResultView from '../views/ResultView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'upload', component: UploadView },
    { path: '/result', name: 'result', component: ResultView },
  ],
})

export default router