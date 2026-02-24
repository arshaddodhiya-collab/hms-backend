#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COOKIE_JAR="${COOKIE_JAR:-/tmp/hms_cookies.txt}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1"
    exit 1
  fi
}

require_cmd curl

pretty_print() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  else
    cat
  fi
}

echo "[1/4] Login as ${ADMIN_USER}"
LOGIN_HTTP_CODE=$(curl -s -o /tmp/hms_login_response.json -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -c "${COOKIE_JAR}" \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")

if [[ "${LOGIN_HTTP_CODE}" != "200" ]]; then
  echo "Login failed with HTTP ${LOGIN_HTTP_CODE}"
  cat /tmp/hms_login_response.json
  exit 1
fi

echo "Login response:"
cat /tmp/hms_login_response.json | pretty_print

echo "[2/4] Validate /auth/me"
ME_HTTP_CODE=$(curl -s -o /tmp/hms_me_response.json -w "%{http_code}" \
  -X GET "${BASE_URL}/api/v1/auth/me" \
  -b "${COOKIE_JAR}")

if [[ "${ME_HTTP_CODE}" != "200" ]]; then
  echo "/auth/me failed with HTTP ${ME_HTTP_CODE}"
  cat /tmp/hms_me_response.json
  exit 1
fi

cat /tmp/hms_me_response.json | pretty_print

echo "[3/4] Create patient"
UNIQ_SUFFIX="$(date +%s)"
PATIENT_HTTP_CODE=$(curl -s -o /tmp/hms_patient_create_response.json -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/patients" \
  -H 'Content-Type: application/json' \
  -b "${COOKIE_JAR}" \
  -d "{\"firstName\":\"Smoke\",\"lastName\":\"Test${UNIQ_SUFFIX}\",\"dob\":\"1990-01-01\",\"gender\":\"MALE\",\"bloodGroup\":\"O_POSITIVE\",\"contact\":\"9876543210\",\"email\":\"smoke${UNIQ_SUFFIX}@example.com\",\"address\":\"Local\"}")

if [[ "${PATIENT_HTTP_CODE}" != "201" ]]; then
  echo "Create patient failed with HTTP ${PATIENT_HTTP_CODE}"
  cat /tmp/hms_patient_create_response.json
  exit 1
fi

cat /tmp/hms_patient_create_response.json | pretty_print

echo "[4/4] Search patients"
SEARCH_HTTP_CODE=$(curl -s -o /tmp/hms_patient_search_response.json -w "%{http_code}" \
  -X GET "${BASE_URL}/api/v1/patients?query=Smoke&page=0&size=5" \
  -b "${COOKIE_JAR}")

if [[ "${SEARCH_HTTP_CODE}" != "200" ]]; then
  echo "Search patients failed with HTTP ${SEARCH_HTTP_CODE}"
  cat /tmp/hms_patient_search_response.json
  exit 1
fi

cat /tmp/hms_patient_search_response.json | pretty_print

echo "Smoke test completed successfully."
