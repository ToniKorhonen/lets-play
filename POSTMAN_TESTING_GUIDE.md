# Postman API Testing Guide - Let's Play

## Overview
This guide will help you test all CRUD operations for Users and Products using Postman.

## Role System
- **Roles are stored in database as**: `USER` or `ADMIN` (without ROLE_ prefix)
- **Display names**: "User" or "Admin" 
- **Spring Security uses**: `ROLE_USER` or `ROLE_ADMIN` internally
- **When registering via API**: Use `"role": "USER"` or `"role": "ADMIN"`

---

## Setup Instructions

### 1. Create a Postman Environment
Create environment variables:
- `baseUrl`: `http://localhost:8080`
- `token`: (will be auto-populated)

### 2. Auto-Save JWT Token
In the Login request, add this to the **Tests** tab:
```javascript
pm.test("Save JWT token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.token);
});
```

---

## Authentication Endpoints

### 1. Register User (POST)
- **URL**: `{{baseUrl}}/api/users`
- **Method**: `POST`
- **Headers**: 
  - `Content-Type: application/json`
- **Body** (raw JSON):
```json
{
  "name": "Test User",
  "email": "user@example.com",
  "password": "Password1!",
  "role": "USER"
}
```

### 2. Register Admin (POST)
- **URL**: `{{baseUrl}}/api/users`
- **Method**: `POST`
- **Headers**: 
  - `Content-Type: application/json`
- **Body** (raw JSON):
```json
{
  "name": "Admin User",
  "email": "admin@example.com",
  "password": "Admin123!",
  "role": "ADMIN"
}
```

### 3. Login (POST)
- **URL**: `{{baseUrl}}/api/auth/login`
- **Method**: `POST`
- **Headers**: 
  - `Content-Type: application/json`
- **Body** (raw JSON):
```json
{
  "email": "admin@example.com",
  "password": "Admin123!"
}
```
- **Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```
- **Note**: Copy the token or use the auto-save script above

---

## User Endpoints (ADMIN only)

### 4. Get All Users (GET)
- **URL**: `{{baseUrl}}/api/users`
- **Method**: `GET`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
- **Required Role**: ADMIN

### 5. Get User by ID (GET)
- **URL**: `{{baseUrl}}/api/users/{userId}`
- **Method**: `GET`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
- **Required Role**: ADMIN
- **Example**: `{{baseUrl}}/api/users/67890abc123`

### 6. Update User (PUT)
- **URL**: `{{baseUrl}}/api/users/{userId}`
- **Method**: `PUT`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Required Role**: ADMIN
- **Body** (raw JSON):
```json
{
  "name": "Updated Name",
  "role": "ADMIN"
}
```

### 7. Delete User (DELETE)
- **URL**: `{{baseUrl}}/api/users/{userId}`
- **Method**: `DELETE`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
- **Required Role**: Authenticated (own account) or ADMIN (any account)

---

## Product Endpoints

### 8. Get All Products (GET) - PUBLIC
- **URL**: `{{baseUrl}}/api/products`
- **Method**: `GET`
- **Headers**: None required (public access)
- **Note**: This is the ONLY endpoint accessible without authentication

### 9. Get Product by ID (GET) - PUBLIC
- **URL**: `{{baseUrl}}/api/products/{productId}`
- **Method**: `GET`
- **Headers**: None required
- **Example**: `{{baseUrl}}/api/products/12345abc`

### 10. Create Product (POST)
- **URL**: `{{baseUrl}}/api/products`
- **Method**: `POST`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Required Role**: Authenticated
- **Body** (raw JSON):
```json
{
  "name": "Gaming Console",
  "description": "Next-gen gaming console",
  "price": 499.99
}
```

### 11. Update Product (PUT)
- **URL**: `{{baseUrl}}/api/products/{productId}`
- **Method**: `PUT`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`
- **Required Role**: Owner or ADMIN
- **Body** (raw JSON):
```json
{
  "name": "Updated Gaming Console",
  "description": "Updated description",
  "price": 449.99
}
```

### 12. Delete Product (DELETE)
- **URL**: `{{baseUrl}}/api/products/{productId}`
- **Method**: `DELETE`
- **Headers**: 
  - `Authorization: Bearer {{token}}`
- **Required Role**: Owner or ADMIN

---

## Testing Scenarios

### Scenario 1: User Registration and Login
1. Register a new user (endpoint #1)
2. Login with that user (endpoint #3)
3. Copy the token from response
4. Try to get all users (endpoint #4) - Should FAIL (403 Forbidden) because user is not ADMIN

### Scenario 2: Admin Operations
1. Register an admin user (endpoint #2)
2. Login as admin (endpoint #3)
3. Get all users (endpoint #4) - Should SUCCEED
4. Create a product (endpoint #10) - Should SUCCEED
5. Update any product (endpoint #11) - Should SUCCEED
6. Delete any product (endpoint #12) - Should SUCCEED

### Scenario 3: Product CRUD as Regular User
1. Login as regular user
2. Get all products (endpoint #8) - Should SUCCEED (public)
3. Create a product (endpoint #10) - Should SUCCEED
4. Update your own product (endpoint #11) - Should SUCCEED
5. Try to update another user's product - Should FAIL (403 Forbidden)
6. Delete your own product (endpoint #12) - Should SUCCEED

### Scenario 4: Public Access
1. Don't login (no token)
2. Get all products (endpoint #8) - Should SUCCEED
3. Get product by ID (endpoint #9) - Should SUCCEED
4. Try to create product (endpoint #10) - Should FAIL (401 Unauthorized)

### Scenario 5: Exception Handling
1. Login as admin
2. Try to get non-existent user: GET `{{baseUrl}}/api/users/invalid-id`
   - Should return 404 with error message
3. Try to update non-existent product: PUT `{{baseUrl}}/api/products/invalid-id`
   - Should return 404 with error message
4. Try to register with duplicate email (endpoint #1)
   - Should return 409 Conflict

---

## Expected HTTP Status Codes

- **200 OK**: Successful GET, PUT
- **201 Created**: Successful POST (creation)
- **204 No Content**: Successful DELETE
- **400 Bad Request**: Validation errors
- **401 Unauthorized**: Missing or invalid token
- **403 Forbidden**: Insufficient permissions (e.g., USER trying to access admin endpoint)
- **404 Not Found**: Resource not found
- **409 Conflict**: Duplicate resource (e.g., email already exists)

---

## Common Issues

### "Full authentication is required"
- **Cause**: Missing or invalid JWT token
- **Solution**: 
  1. Make sure you're logged in (endpoint #3)
  2. Copy the entire token from the response
  3. Add header: `Authorization: Bearer YOUR_TOKEN_HERE`
  4. Note: There must be a space between "Bearer" and the token

### "Access is denied" or 403 Forbidden
- **Cause**: Insufficient role permissions
- **Solution**: 
  1. Check if endpoint requires ADMIN role
  2. Login with admin account
  3. Verify your user has `"role": "ADMIN"` in database

### Invalid token
- **Cause**: Token expired (24 hours) or malformed
- **Solution**: Login again to get a new token

---

## Web Interface (Bonus)

### Admin Features in Web UI
1. Login to the web app at `http://localhost:8080/login`
2. Use admin credentials
3. Navigate to home page
4. Click "View All Users" button (only visible to admins)
5. View, manage, and delete users from the UI

### Regular User Features
- View products at `/products`
- Create new products (when logged in)
- Edit/delete your own products
- View all products (public access)

---

## Tips for Postman

1. **Create a Collection**: Organize all requests in a collection named "Let's Play API"

2. **Use Pre-request Scripts**: Auto-login before each request
```javascript
// In Collection > Pre-request Script
pm.sendRequest({
    url: pm.environment.get("baseUrl") + "/api/auth/login",
    method: 'POST',
    header: 'Content-Type: application/json',
    body: {
        mode: 'raw',
        raw: JSON.stringify({
            email: "admin@example.com",
            password: "Admin123!"
        })
    }
}, function (err, response) {
    pm.environment.set("token", response.json().token);
});
```

3. **Test Scripts**: Add assertions
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has token", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.token).to.exist;
});
```

4. **Export and Share**: Export your collection and environment for team collaboration

