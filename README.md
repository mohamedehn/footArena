# 📘 Guide d'Utilisation API FootArena

## 🚀 Démarrage Rapide

### 1. Créer un compte
```bash
POST /account/register
{
  "firstName": "John",
  "lastName": "Doe", 
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```
### 2. Authentification
```bash
POST /auth/login
{
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "rememberMe": false
}
```

### Authentification - response
```bash
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900,
    "user": { ... }
  }
}
```
### 3. Endpoints protégées
```bash
GET /auth/me
Authorization: Bearer YOUR_ACCESS_TOKEN

### 4. Rafraîchir le token
```bash
POST /auth/refresh
{
  "refreshToken": "YOUR_REFRESH_TOKEN"
}
```
