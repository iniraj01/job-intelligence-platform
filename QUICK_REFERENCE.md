# Quick Reference: Authentication System

## 🔑 Environment Variables to Set (in application.properties)

```properties
# REPLACE THESE:
recaptcha.secret=YOUR_RECAPTCHA_SECRET_KEY
recaptcha.site=YOUR_RECAPTCHA_SITE_KEY
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
jwt.secret=YOUR_RANDOM_SECRET_MIN_32_CHARS
```

## 📱 API Endpoints Quick Reference

### 1. User Signup
```bash
POST /api/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password@123",
  "phone": "+1234567890",
  "captchaToken": "RECAPTCHA_RESPONSE_TOKEN"
}
```
**Response:** User created, OTP sent to email

---

### 2. Verify OTP
```bash
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "john@example.com",
  "otp": "123456"
}
```
**Response:** Email verified, ready to login

---

### 3. User Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Password@123",
  "captchaToken": "RECAPTCHA_RESPONSE_TOKEN"
}
```
**Response:** JWT Token + User Profile
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "name": "John Doe",
    "email": "john@example.com",
    "emailVerified": true
  }
}
```

---

### 4. Access Protected Route
```bash
GET /api/auth/protected/profile
Authorization: Bearer YOUR_JWT_TOKEN
```

---

### 5. Change Password
```bash
POST /api/auth/change-password?email=john@example.com&oldPassword=OldPass&newPassword=NewPass123
Authorization: Bearer YOUR_JWT_TOKEN
```

---

### 6. Resend OTP
```bash
POST /api/auth/resend-otp?email=john@example.com
```

---

## 🌐 Frontend JavaScript Snippets

### Get reCAPTCHA Token
```javascript
// Add to HTML: <script src="https://www.google.com/recaptcha/api.js"></script>
// Add to form: <div class="g-recaptcha" data-sitekey="YOUR_SITE_KEY"></div>

const captchaToken = grecaptcha.getResponse();
```

### Signup Call
```javascript
const signup = async (name, email, password, phone) => {
  const captchaToken = grecaptcha.getResponse();
  
  const response = await fetch('/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name, email, password, phone, captchaToken
    })
  });
  
  return response.json();
};
```

### Login Call
```javascript
const login = async (email, password) => {
  const captchaToken = grecaptcha.getResponse();
  
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, captchaToken })
  });
  
  const data = await response.json();
  
  if (data.success) {
    localStorage.setItem('authToken', data.token);
    return data;
  }
  
  throw new Error(data.message);
};
```

### Verify OTP
```javascript
const verifyOtp = async (email, otp) => {
  const response = await fetch('/api/auth/verify-otp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, otp })
  });
  
  return response.json();
};
```

### Access Protected Resource
```javascript
const getProfile = async () => {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch('/api/auth/protected/profile', {
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  if (response.ok) {
    return response.text();
  } else {
    throw new Error('Unauthorized');
  }
};
```

---

## 📊 Database Collections

### Users
```javascript
db.users.find()
[
  {
    _id: ObjectId("..."),
    name: "John Doe",
    email: "john@example.com",
    password: "$2a$10$...", // BCrypt hash
    phone: "+1234567890",
    emailVerified: true,
    accountEnabled: true,
    createdAt: ISODate("2024-01-01T10:00:00Z"),
    updatedAt: ISODate("2024-01-01T10:00:00Z")
  }
]
```

### OTP Data
```javascript
db.otp_data.find()
[
  {
    _id: ObjectId("..."),
    email: "john@example.com",
    otp: "123456",
    expiry: ISODate("2024-01-01T10:05:00Z"),
    attempts: 1,
    verified: false,
    createdAt: ISODate("2024-01-01T10:00:00Z")
  }
]
```

---

## 🔒 Security Rules

| Rule | Details |
|------|---------|
| Password Min Length | 8 characters |
| OTP Length | 6 digits |
| OTP Validity | 5 minutes |
| OTP Max Attempts | 5 attempts |
| JWT Expiry | 24 hours |
| CAPTCHA Required | On signup & login |
| Email Verified | Required for login |

---

## ❌ Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Invalid input data"
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Invalid email or password"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "You can only change your own password"
}
```

### 409 Conflict
```json
{
  "success": false,
  "message": "Email already registered"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Server error occurred"
}
```

---

## 🧪 Test With cURL

### Test Signup
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "TestPass123",
    "phone": "+1234567890",
    "captchaToken": "test-token"
  }'
```

### Test Health
```bash
curl http://localhost:8080/api/auth/health
```

### Test Protected Route
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/auth/protected/profile
```

---

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| Email not sending | Check Gmail app password, enable 2FA |
| CAPTCHA failing | Verify reCAPTCHA keys in properties |
| OTP expired | OTP valid for 5 minutes only |
| Token invalid | Re-login to get new token |
| MongoDB connection error | Ensure MongoDB is running on localhost:27017 |

---

## 📚 File Locations

| File | Location |
|------|----------|
| Configuration | `src/main/resources/application.properties` |
| Auth Service | `src/main/java/.../service/AuthenticationService.java` |
| JWT Service | `src/main/java/.../service/JwtTokenService.java` |
| OTP Service | `src/main/java/.../service/OtpService.java` |
| Controller | `src/main/java/.../controller/AuthController.java` |
| Security Config | `src/main/java/.../config/SecurityConfig.java` |
| User Model | `src/main/java/.../model/User.java` |

---

## 💾 Sample Request/Response

### Complete Signup Flow

**1. Signup Request:**
```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "SecurePass@123",
  "phone": "+1-555-0123",
  "captchaToken": "recaptcha_token_here"
}
```

**2. Signup Response:**
```json
{
  "success": true,
  "message": "User registered successfully. Please verify your email with OTP sent to alice@example.com"
}
```

**3. Check Email for OTP: 123456**

**4. Verify OTP Request:**
```json
{
  "email": "alice@example.com",
  "otp": "123456"
}
```

**5. Verify OTP Response:**
```json
{
  "success": true,
  "message": "Email verified successfully. You can now login."
}
```

**6. Login Request:**
```json
{
  "email": "alice@example.com",
  "password": "SecurePass@123",
  "captchaToken": "recaptcha_token_here"
}
```

**7. Login Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "name": "Alice",
    "email": "alice@example.com",
    "phone": "+1-555-0123",
    "emailVerified": true,
    "accountEnabled": true
  }
}
```

---

## 🎯 Success Criteria Checklist

- [ ] All placeholders replaced in application.properties
- [ ] Project builds successfully
- [ ] MongoDB is running
- [ ] Health endpoint returns 200 OK
- [ ] Can create new user account
- [ ] OTP email is received
- [ ] Can verify OTP successfully
- [ ] Can login with correct credentials
- [ ] JWT token is generated
- [ ] Protected routes require token
- [ ] Invalid token returns 401
- [ ] Password hashing works (no plain text in DB)

---

**Ready to use!** 🚀
