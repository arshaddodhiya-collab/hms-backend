#!/usr/bin/env bash
set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
  echo "java not found. Install Java 17+ and retry."
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql client not found. Install MySQL client/server and retry."
  exit 1
fi

export DB_USERNAME="${DB_USERNAME:-Arashad}"
export DB_PASSWORD="${DB_PASSWORD:-Arashad@6139}"
export JWT_SECRET="${JWT_SECRET:-VGhpc0lzQVNlY3JldEtleVGhhdElzTG9uZ0Vub3VnaEZvckhTMjU2QWxnb3JpdGht}"
export SECURE_COOKIE="${SECURE_COOKIE:-false}"

echo "Using DB_USERNAME=${DB_USERNAME}"
echo "SECURE_COOKIE=${SECURE_COOKIE}"
echo "Starting Spring Boot app..."

./mvnw spring-boot:run
