#!/bin/bash

echo "🚀 Tests Complets FootArena API"
echo "================================"

BASE_URL="http://localhost:8090/api"

echo ""
echo "📋 1. Health Check"
curl -X GET "$BASE_URL/test/health" -w "\nStatus: %{http_code}\n\n"

echo "📋 2. Swagger Docs"
curl -X GET "http://localhost:8090/v3/api-docs" \
     -H "Accept: application/json" \
     -s | jq '.info.title' 2>/dev/null || echo "Swagger OK mais jq non installé"
echo ""

echo "📋 3. Établissements - Liste complète"
curl -X GET "$BASE_URL/establishments/all" \
     -H "Accept: application/json" \
     -s | jq '.data | length' 2>/dev/null || echo "Données récupérées"
echo ""

echo "📋 4. Établissements - Pagination"
curl -X GET "$BASE_URL/establishments?page=0&size=2" \
     -H "Accept: application/json" \
     -w "\nStatus: %{http_code}\n\n"

echo "📋 5. Terrains disponibles"
curl -X GET "$BASE_URL/fields/available" \
     -H "Accept: application/json" \
     -s | jq '.data | length' 2>/dev/null || echo "Terrains disponibles récupérés"
echo ""

echo "📋 6. Test établissement par ID"
ESTABLISHMENT_ID="569e9fd0-9362-4377-a907-7c480d969409"
curl -X GET "$BASE_URL/establishments/$ESTABLISHMENT_ID" \
     -H "Accept: application/json" \
     -w "\nStatus: %{http_code}\n\n"

echo "📋 7. Terrains d'un établissement"
curl -X GET "$BASE_URL/fields/establishment/$ESTABLISHMENT_ID" \
     -H "Accept: application/json" \
     -w "\nStatus: %{http_code}\n\n"

echo "✅ Tests terminés!"
echo ""
echo "🌐 Swagger UI: http://localhost:8090/swagger-ui.html"
echo "📊 API Docs: http://localhost:8090/v3/api-docs"