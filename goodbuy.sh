#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$ROOT_DIR/.goodbuy-runtime"
LOG_DIR="$RUNTIME_DIR/logs"
ENV_FILE="$ROOT_DIR/.env.local"

mkdir -p "$LOG_DIR"

log() {
  printf '[goodBuy] %s\n' "$*"
}

load_environment() {
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  fi
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "필요한 명령을 찾지 못했습니다: $1"
    exit 1
  fi
}

pid_file() {
  printf '%s/%s.pid' "$RUNTIME_DIR" "$1"
}

is_running() {
  local file
  file="$(pid_file "$1")"
  [[ -f "$file" ]] && kill -0 "$(cat "$file")" 2>/dev/null
}

stop_pid() {
  local pid="$1"

  kill -TERM "$pid" 2>/dev/null || true
  pkill -TERM -P "$pid" 2>/dev/null || true

  for _ in {1..20}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      return
    fi
    sleep 0.1
  done

  kill -KILL "$pid" 2>/dev/null || true
  pkill -KILL -P "$pid" 2>/dev/null || true
}

stop_managed_service() {
  local name="$1"
  local file
  local pid

  file="$(pid_file "$name")"
  if [[ ! -f "$file" ]]; then
    return
  fi

  pid="$(cat "$file")"
  if kill -0 "$pid" 2>/dev/null; then
    stop_pid "$pid"
  fi
  rm -f "$file"
}

stop_repo_listener() {
  local port="$1"
  local pid
  local cwd

  while IFS= read -r pid; do
    [[ -n "$pid" ]] || continue
    cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p' | head -n 1)"
    case "$cwd" in
      "$ROOT_DIR"|"$ROOT_DIR"/*)
        log "기존 프로젝트 프로세스 종료: port=$port pid=$pid"
        stop_pid "$pid"
        ;;
      *)
        log "포트 $port 를 다른 프로그램(pid=$pid)이 사용 중입니다. 해당 프로그램을 먼저 종료해주세요."
        return 1
        ;;
    esac
  done < <(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u)
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local pid="$3"

  for _ in {1..120}; do
    if curl --max-time 1 --silent --fail "$url" >/dev/null 2>&1; then
      log "$name 준비 완료: $url"
      return 0
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      log "$name 프로세스가 시작 도중 종료됐습니다."
      return 1
    fi
    sleep 0.25
  done

  log "$name 시작 확인 시간이 초과됐습니다."
  return 1
}

show_log_tail() {
  local name="$1"
  local file="$LOG_DIR/$name.log"

  if [[ -f "$file" ]]; then
    log "$name 최근 로그"
    tail -n 30 "$file"
  fi
}

validate_environment() {
  require_command curl
  require_command lsof
  require_command npm

  if [[ ! -x "$ROOT_DIR/backend-python/.venv/bin/python" ]]; then
    log "Python 가상환경이 없습니다: backend-python/.venv"
    exit 1
  fi
  if [[ ! -x "$ROOT_DIR/backend-spring/gradlew" ]]; then
    log "Gradle Wrapper가 없습니다: backend-spring/gradlew"
    exit 1
  fi
  if [[ ! -d "$ROOT_DIR/frontend/goodBuy/node_modules" ]]; then
    log "프론트엔드 패키지가 없습니다. frontend/goodBuy에서 npm install을 먼저 실행해주세요."
    exit 1
  fi

  local variable
  for variable in SUPABASE_DB_URL SUPABASE_DB_USER SUPABASE_DB_PASSWORD; do
    if [[ -z "${!variable:-}" ]]; then
      log "$ENV_FILE 에 $variable 설정이 필요합니다."
      exit 1
    fi
  done
}

stop_services() {
  local quiet="${1:-false}"

  stop_managed_service frontend
  stop_managed_service spring
  stop_managed_service python

  stop_repo_listener 5173 || return 1
  stop_repo_listener 8080 || return 1
  stop_repo_listener 8000 || return 1

  if [[ "$quiet" != "true" ]]; then
    log "프론트엔드, Spring, Python을 모두 종료했습니다."
  fi
}

start_services() {
  local python_pid
  local spring_pid
  local frontend_pid

  load_environment
  validate_environment
  stop_services true

  log "Python OCR 시작 중..."
  (
    cd "$ROOT_DIR/backend-python" || exit 1
    nohup env OCR_MODE="${PYTHON_OCR_MODE:-paddle}" \
      .venv/bin/python -m uvicorn app:app --host 127.0.0.1 --port 8000 \
      >"$LOG_DIR/python.log" 2>&1 &
    echo $! >"$(pid_file python)"
  )
  python_pid="$(cat "$(pid_file python)")"
  if ! wait_for_url "Python OCR" "http://127.0.0.1:8000/health" "$python_pid"; then
    show_log_tail python
    stop_services true
    exit 1
  fi

  log "Spring Backend 시작 중..."
  (
    cd "$ROOT_DIR/backend-spring" || exit 1
    nohup env \
      SPRING_PROFILES_ACTIVE=supabase \
      OCR_MODE=python \
      OCR_BASE_URL=http://127.0.0.1:8000 \
      SUPABASE_DB_URL="$SUPABASE_DB_URL" \
      SUPABASE_DB_USER="$SUPABASE_DB_USER" \
      SUPABASE_DB_PASSWORD="$SUPABASE_DB_PASSWORD" \
      ./gradlew bootRun >"$LOG_DIR/spring.log" 2>&1 &
    echo $! >"$(pid_file spring)"
  )
  spring_pid="$(cat "$(pid_file spring)")"
  if ! wait_for_url "Spring Backend" "http://127.0.0.1:8080/actuator/health" "$spring_pid"; then
    show_log_tail spring
    stop_services true
    exit 1
  fi

  log "Vue Frontend 시작 중..."
  (
    cd "$ROOT_DIR/frontend/goodBuy" || exit 1
    nohup env \
      VITE_API_BASE_URL=http://127.0.0.1:8080 \
      VITE_ANALYSIS_MODE=backend \
      npm run dev -- --host 127.0.0.1 >"$LOG_DIR/frontend.log" 2>&1 &
    echo $! >"$(pid_file frontend)"
  )
  frontend_pid="$(cat "$(pid_file frontend)")"
  if ! wait_for_url "Vue Frontend" "http://127.0.0.1:5173/" "$frontend_pid"; then
    show_log_tail frontend
    stop_services true
    exit 1
  fi

  log "전체 실행 완료: http://127.0.0.1:5173"
  log "로그 위치: $LOG_DIR"
}

show_status() {
  local name
  local port
  local pid

  for entry in "frontend:5173" "spring:8080" "python:8000"; do
    name="${entry%%:*}"
    port="${entry##*:}"
    pid="$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -n 1)"
    if [[ -n "$pid" ]]; then
      log "$name 실행 중: port=$port pid=$pid"
    else
      log "$name 중지됨: port=$port"
    fi
  done
}

show_logs() {
  tail -n 80 -f "$LOG_DIR/python.log" "$LOG_DIR/spring.log" "$LOG_DIR/frontend.log"
}

case "${1:-}" in
  start)
    start_services
    ;;
  stop)
    stop_services
    ;;
  restart)
    stop_services true
    start_services
    ;;
  status)
    show_status
    ;;
  logs)
    show_logs
    ;;
  *)
    printf '사용법: %s {start|stop|restart|status|logs}\n' "$0"
    exit 1
    ;;
esac
