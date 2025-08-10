#!/bin/bash

echo "🚀 Tests des endpoints d'authentification"
echo "=========================================="

BASE_URL="http://localhost:8090"

echo ""
echo "1. Test Swagger Docs"
curl -X GET "$BASE_URL/v3/api-docs" \
     -H "Accept: application/json" \
     -w "\nStatus: %{http_code}\n\n" \
     -s | jq '.info.title' 2>/dev/null || echo "API Docs accessible"

echo ""
echo "2. Test Registration (Public)"
curl -X POST "$BASE_URL/account/register" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "firstName": "Test",
       "lastName": "User",
       "email": "test@example.com",
       "password": "TestPassword123!"
     }' \
     -w "\nStatus: %{http_code}\n\n"

echo "3. Test Login (Public)"
curl -X POST "$BASE_URL/auth/login" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "TestPassword123!",
       "rememberMe": false
     }' \
     -w "\nStatus: %{http_code}\n\n"

echo "4. Test Validation Endpoint (Public)"
curl -X GET "$BASE_URL/auth/validate" \
     -H "Authorization: Bearer invalid-token" \
     -H "Accept: application/json" \
     -w "\nStatus: %{http_code} (401 expected for invalid token)\n\n"

echo "5. Test Etablissements (Public)"
curl -X GET "$BASE_URL/establishments/all" \
     -H "Accept: application/json" \
     -w "\nStatus: %{http_code}\n\n"

echo "✅ Tests terminés!"
echo ""
echo "🌐 Swagger UI: http://localhost:8090/swagger-ui.html"
echo "📊 API Docs: http://localhost:8090/v3/api-docs"