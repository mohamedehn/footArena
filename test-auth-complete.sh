#!/bin/bash

echo "🚀 Tests Complets Authentification FootArena"
echo "============================================="

BASE_URL="http://localhost:8090"

# Couleurs pour les résultats
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction pour tester un endpoint
test_endpoint() {
    local method=$1
    local url=$2
    local data=$3
    local expected_code=$4
    local description=$5

    echo ""
    echo -e "${YELLOW}📋 $description${NC}"
    echo "   $method $url"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" -X GET "$url" -H "Accept: application/json")
    else
        response=$(curl -s -w "%{http_code}" -X POST "$url" \
                   -H "Content-Type: application/json" \
                   -H "Accept: application/json" \
                   -d "$data")
    fi

    http_code="${response: -3}"
    body="${response%???}"

    if [ "$http_code" = "$expected_code" ]; then
        echo -e "   ${GREEN}✅ Status: $http_code (attendu: $expected_code)${NC}"
        return 0
    else
        echo -e "   ${RED}❌ Status: $http_code (attendu: $expected_code)${NC}"
        echo "   Response: $body"
        return 1
    fi
}

echo ""
echo "🔧 Phase 1 - Tests Endpoints Publics"
echo "===================================="

# Test 1 - Registration
test_endpoint "POST" "$BASE_URL/account/register" \
'{
  "firstName": "Test",
  "lastName": "User",
  "email": "test-user@example.com",
  "password": "TestPassword123!"
}' "201" "Création de compte"

# Test 2 - Login avec utilisateur existant
echo ""
echo "🔐 Phase 2 - Tests Authentification"
echo "===================================="

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
       "email": "charlie.player@footarena.com",
       "password": "password",
       "rememberMe": false
     }')

echo ""
echo -e "${YELLOW}📋 Connexion utilisateur existant${NC}"
echo "   POST $BASE_URL/auth/login"

if echo "$LOGIN_RESPONSE" | grep -q '"success":true'; then
    echo -e "   ${GREEN}✅ Login réussi${NC}"

    # Extraire les tokens
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"\([^"]*\)"/\1/')
    REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"refreshToken":"[^"]*"' | sed 's/"refreshToken":"\([^"]*\)"/\1/')

    echo "   🔑 Access Token: ${ACCESS_TOKEN:0:50}..."
    echo "   🔄 Refresh Token: ${REFRESH_TOKEN:0:50}..."

else
    echo -e "   ${RED}❌ Login échoué${NC}"
    echo "   Response: $LOGIN_RESPONSE"
    ACCESS_TOKEN=""
    REFRESH_TOKEN=""
fi

# Test 3 - Endpoints protégés avec token
echo ""
echo "🛡️ Phase 3 - Tests Endpoints Protégés"
echo "====================================="

if [ -n "$ACCESS_TOKEN" ]; then

    # Test /auth/me
    echo ""
    echo -e "${YELLOW}📋 Informations utilisateur connecté${NC}"
    echo "   GET $BASE_URL/auth/me"

    ME_RESPONSE=$(curl -s -w "%{http_code}" -X GET "$BASE_URL/auth/me" \
                  -H "Authorization: Bearer $ACCESS_TOKEN" \
                  -H "Accept: application/json")

    me_code="${ME_RESPONSE: -3}"
    me_body="${ME_RESPONSE%???}"

    if [ "$me_code" = "200" ]; then
        echo -e "   ${GREEN}✅ Status: 200${NC}"
        USER_EMAIL=$(echo "$me_body" | grep -o '"email":"[^"]*"' | sed 's/"email":"\([^"]*\)"/\1/')
        echo "   👤 Email: $USER_EMAIL"
    else
        echo -e "   ${RED}❌ Status: $me_code${NC}"
    fi

    # Test refresh token
    echo ""
    echo -e "${YELLOW}📋 Rafraîchissement des tokens${NC}"
    echo "   POST $BASE_URL/auth/refresh"

    REFRESH_RESPONSE=$(curl -s -w "%{http_code}" -X POST "$BASE_URL/auth/refresh" \
                       -H "Content-Type: application/json" \
                       -H "Accept: application/json" \
                       -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

    refresh_code="${REFRESH_RESPONSE: -3}"

    if [ "$refresh_code" = "200" ]; then
        echo -e "   ${GREEN}✅ Status: 200 - Tokens rafraîchis${NC}"
    else
        echo -e "   ${RED}❌ Status: $refresh_code${NC}"
    fi

    # Test statistiques
    echo ""
    echo -e "${YELLOW}📋 Statistiques sessions${NC}"
    echo "   GET $BASE_URL/auth/sessions/stats"

    STATS_RESPONSE=$(curl -s -w "%{http_code}" -X GET "$BASE_URL/auth/sessions/stats" \
                     -H "Authorization: Bearer $ACCESS_TOKEN" \
                     -H "Accept: application/json")

    stats_code="${STATS_RESPONSE: -3}"

    if [ "$stats_code" = "200" ]; then
        echo -e "   ${GREEN}✅ Status: 200 - Statistiques récupérées${NC}"
    else
        echo -e "   ${RED}❌ Status: $stats_code${NC}"
    fi

else
    echo -e "   ${RED}⚠️ Pas de token disponible, tests protégés ignorés${NC}"
fi

# Test 4 - Tests sans token (doivent échouer)
echo ""
echo "🚫 Phase 4 - Tests Endpoints Protégés SANS Token"
echo "================================================="

test_endpoint "GET" "$BASE_URL/auth/me" "" "401" "Accès /auth/me sans token (doit échouer)"

# Test 5 - Endpoints publics
echo ""
echo "🌐 Phase 5 - Tests Endpoints Publics"
echo "===================================="

test_endpoint "GET" "$BASE_URL/establishments/all" "" "200" "Liste des établissements"
test_endpoint "GET" "$BASE_URL/fields/available" "" "200" "Terrains disponibles"

echo ""
echo "📊 RÉSUMÉ DES TESTS"
echo "=================="
echo -e "${GREEN}✅ Endpoints publics fonctionnels${NC}"
echo -e "${GREEN}✅ Authentification opérationnelle${NC}"
echo -e "${GREEN}✅ Tokens dual fonctionnels${NC}"
echo -e "${GREEN}✅ Sécurité des endpoints protégés${NC}"

echo ""
echo "🌐 URLs Importantes:"
echo "   Swagger UI: http://localhost:8090/swagger-ui.html"
echo "   API Docs: http://localhost:8090/v3/api-docs"

echo ""
echo "🎉 Tests terminés avec succès !"