import { createMockAnalysis } from '@/mocks/analysis'

const ANALYSIS_ENDPOINT = '/api/v1/analyses'

export const analysisMode = import.meta.env.VITE_ANALYSIS_MODE === 'backend' ? 'backend' : 'mock'

/**
 * 업로드 화면에서 사용하는 단일 분석 진입점입니다.
 * 기본은 화면 개발용 mock이며, VITE_ANALYSIS_MODE=backend이면 Spring을 호출합니다.
 */
export async function analyzeExpenses({ ownerName, images }) {
  if (analysisMode === 'mock') {
    return createMockAnalysis({ ownerName, images })
  }

  const formData = new FormData()
  formData.append('ownerName', ownerName)
  images.forEach((image) => formData.append('images', image))

  const baseUrl = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  let response

  try {
    response = await fetch(`${baseUrl}${ANALYSIS_ENDPOINT}`, {
      method: 'POST',
      body: formData,
    })
  } catch (error) {
    throw new AnalysisApiError(
      'Spring 백엔드에 연결할 수 없습니다. 서버 실행 상태를 확인해주세요.',
      0,
      'NETWORK_ERROR',
      error,
    )
  }

  if (!response.ok) {
    const problem = await readProblemDetail(response)
    throw new AnalysisApiError(
      problem.detail || `분석 요청에 실패했습니다. (${response.status})`,
      response.status,
      problem.code || 'ANALYSIS_FAILED',
    )
  }

  return response.json()
}

async function readProblemDetail(response) {
  try {
    return await response.json()
  } catch {
    return {}
  }
}

export class AnalysisApiError extends Error {
  constructor(message, status, code, cause) {
    super(message, { cause })
    this.name = 'AnalysisApiError'
    this.status = status
    this.code = code
  }
}
