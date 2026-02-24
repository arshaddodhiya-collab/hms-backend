#!/bin/bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hms.com","password":"123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"doctor@hms.com","password":"123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

echo "Token: $TOKEN"
curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/lab-requests/1/pdf > output.pdf
