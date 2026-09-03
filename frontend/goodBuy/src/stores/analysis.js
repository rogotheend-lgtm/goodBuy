import { ref, shallowRef } from 'vue'

// 로그인과 결과 조회 API가 없는 현재 PoC에서는 새로고침 전까지만 결과를 보관합니다.
const analysis = shallowRef(null)
const ownerName = ref('')
const sourceMode = ref('mock')

export function useAnalysisStore() {
  function saveResult(result, name, mode) {
    analysis.value = result
    ownerName.value = name
    sourceMode.value = mode
  }

  function clearResult() {
    analysis.value = null
    ownerName.value = ''
    sourceMode.value = 'mock'
  }

  return { analysis, ownerName, sourceMode, saveResult, clearResult }
}
