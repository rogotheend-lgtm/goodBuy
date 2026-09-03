import { createMockAnalysis } from '@/mocks/analysis'

const ANALYSIS_ENDPOINT = '/api/v1/analyses'
const REQUEST_OVERHEAD_TIMEOUT_MILLISECONDS = 15_000
const OCR_TIMEOUT_PER_IMAGE_MILLISECONDS = 65_000

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
  const controller = new AbortController()
  // Spring은 이미지를 한 장씩 처리하며 Python 호출당 최대 60초를 기다립니다.
  // 브라우저가 Spring보다 먼저 요청을 끊지 않도록 전체 제한 시간을 그보다 길게 둡니다.
  const timeoutMilliseconds =
    REQUEST_OVERHEAD_TIMEOUT_MILLISECONDS + images.length * OCR_TIMEOUT_PER_IMAGE_MILLISECONDS
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMilliseconds)
  let response

  try {
    response = await fetch(`${baseUrl}${ANALYSIS_ENDPOINT}`, {
      method: 'POST',
      body: formData,
      signal: controller.signal,
    })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new AnalysisApiError(
        '분석 시간이 너무 오래 걸려 요청을 중단했습니다. 잠시 후 다시 시도해주세요.',
        0,
        'ANALYSIS_TIMEOUT',
        error,
      )
    }
    throw new AnalysisApiError(
      'Spring 백엔드에 연결할 수 없습니다. 서버 실행 상태를 확인해주세요.',
      0,
      'NETWORK_ERROR',
      error,
    )
  } finally {
    window.clearTimeout(timeoutId)
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
