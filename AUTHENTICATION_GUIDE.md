# Secure Authentication System with CAPTCHA and Email OTP

## Overview
This Spring Boot application implements a complete secure authentication system with:
- **Google reCAPTCHA v2** for bot prevention
- **Email OTP** (One-Time Password) for email verification
- **JWT (JSON Web Token)** for session management
- **Password Hashing** using BCrypt
- **Protected Routes** with token validation

---

## 🔐 Security Features

### 1. User Registration (Signup)
- Validates input data (name, email, password)
- Verifies CAPTCHA before signup
- Hashes password using BCrypt
- Generates 6-digit OTP for email verification
- Sends OTP to registered email

### 2. Email Verification
- User receives OTP via email
- OTP expires in 5 minutes
- Maximum 5 verification attempts
- Resend OTP functionality

### 3. User Login
- CAPTCHA verification required
- Password validation with BCrypt hash
- Checks if email is verified
- Generates JWT token for authenticated sessions
- Returns user profile in response

### 4. Password Change
- Protected route (requires JWT token)
- Verifies old password
- Updates password with new hash

### 5. Protected Routes
- JWT token validation on each request
- Extracts user email from token
- Restricts access to authenticated users only

---

## 📋 Configuration Setup

### Step 1: Replace Placeholders in `application.properties`

Open [src/main/resources/application.properties](src/main/resources/application.properties) and replace these values:

```properties
# Google reCAPTCHA (Get from: https://www.google.com/recaptcha/admin)
recaptcha.secret=YOUR_RECAPTCHA_SECRET_KEY
recaptcha.site=YOUR_RECAPTCHA_SITE_KEY

# Email Configuration (Gmail SMTP)
spring.mail.username=YOUR_EMAIL_ADDRESS
spring.mail.password=YOUR_APP_PASSWORD_OR_PASSWORD

# JWT Secret (Use a strong random string)
jwt.secret=YOUR_VERY_LONG_RANDOM_SECRET_KEY_MIN_32_CHARS

# MongoDB Connection (if using local)
spring.data.mongodb.uri=mongodb://localhost:27017/jobdb
```

### Step 2: Get Google reCAPTCHA Keys

1. Go to [Google reCAPTCHA Admin Console](https://www.google.com/recaptcha/admin)
2. Create a new site with reCAPTCHA v2 (Checkbox variant)
3. Get the **Site Key** and **Secret Key**
4. Replace in application.properties

### Step 3: Generate App Password for Gmail

1. Enable 2-Factor Authentication on Gmail
2. Go to [App Passwords](https://myaccount.google.com/apppasswords)
3. Select Mail and Windows (or Linux)
4. Generate and copy the 16-character password
5. Use this as `spring.mail.password` (NOT your Gmail password)

### Step 4: Generate JWT Secret

Generate a strong random key:

```bash
# Using OpenSSL
openssl rand -base64 32

# Or using Java
java -jar -cp . -c "java.util.Base64.getEncoder().encodeToString(new java.security.SecureRandom().generateSeed(32))" | head -c 43
```

---

## 🚀 API Endpoints

### Base URL
```
http://localhost:8080/api/auth
```

### 1. **Signup (Register User)**
```
POST /api/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePassword123",
  "phone": "+1234567890",
  "captchaToken": "RECAPTCHA_TOKEN_FROM_FRONTEND"
}

Response (201 Created):
{
  "success": true,
  "message": "User registered successfully. Please verify your email with OTP sent to john@example.com"
}
```

### 2. **Verify OTP**
```
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "john@example.com",
  "otp": "123456"
}

Response (200 OK):
{
  "success": true,
  "message": "Email verified successfully. You can now login."
}
```

### 3. **Resend OTP**
```
POST /api/auth/resend-otp?email=john@example.com

Response (200 OK):
{
  "success": true,
  "message": "OTP sent successfully to john@example.com"
}
```

### 4. **Login**
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePassword123",
  "captchaToken": "RECAPTCHA_TOKEN_FROM_FRONTEND"
}

Response (200 OK):
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+1234567890",
    "emailVerified": true,
    "accountEnabled": true
  }
}
```

### 5. **Change Password (Protected)**
```
POST /api/auth/change-password?email=john@example.com&oldPassword=SecurePassword123&newPassword=NewPassword456
Authorization: Bearer JWT_TOKEN

Response (200 OK):
{
  "success": true,
  "message": "Password changed successfully"
}
```

### 6. **Protected Profile Endpoint**
```
GET /api/auth/protected/profile
Authorization: Bearer JWT_TOKEN

Response (200 OK):
{
  "Welcome john@example.com! This is a protected resource."
}
```

### 7. **Health Check**
```
GET /api/auth/health

Response (200 OK):
{
  "Authentication service is running"
}
```

---

## 🔧 Frontend Integration

### Include reCAPTCHA Script
Add to your HTML `<head>`:
```html
<script src="https://www.google.com/recaptcha/api.js" async defer></script>
```

### Signup Form with CAPTCHA
```html
<form id="signupForm">
  <input type="text" id="name" placeholder="Full Name" required>
  <input type="email" id="email" placeholder="Email" required>
  <input type="password" id="password" placeholder="Password (min 8 chars)" required>
  <input type="tel" id="phone" placeholder="Phone Number">
  
  <div class="g-recaptcha" data-sitekey="YOUR_RECAPTCHA_SITE_KEY"></div>
  <button type="submit">Sign Up</button>
</form>

<script>
document.getElementById('signupForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  
  const captchaToken = grecaptcha.getResponse();
  
  const response = await fetch('/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: document.getElementById('name').value,
      email: document.getElementById('email').value,
      password: document.getElementById('password').value,
      phone: document.getElementById('phone').value,
      captchaToken: captchaToken
    })
  });
  
  const data = await response.json();
  console.log(data);
});
</script>
```

### OTP Verification
```html
<form id="otpForm">
  <input type="email" id="otpEmail" placeholder="Email" required>
  <input type="text" id="otp" placeholder="Enter OTP" maxlength="6" required>
  <button type="submit">Verify OTP</button>
</form>

<script>
document.getElementById('otpForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  
  const response = await fetch('/api/auth/verify-otp', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: document.getElementById('otpEmail').value,
      otp: document.getElementById('otp').value
    })
  });
  
  const data = await response.json();
  if (data.success) {
    alert('Email verified! You can now login.');
  } else {
    alert('Invalid OTP: ' + data.message);
  }
});
</script>
```

### Login with Protected Access
```html
<script>
async function loginUser(email, password, captchaToken) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: email,
      password: password,
      captchaToken: captchaToken
    })
  });
  
  const data = await response.json();
  if (data.success) {
    // Save token
    localStorage.setItem('authToken', data.token);
    // Redirect to dashboard
    window.location.href = '/dashboard';
  } else {
    console.error(data.message);
  }
}

async function accessProtectedResource() {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch('/api/auth/protected/profile', {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  if (response.ok) {
    const data = await response.text();
    console.log(data);
  } else {
    console.error('Unauthorized');
  }
}
</script>
```

---

## 📦 Dependencies Added

The following dependencies have been added to `pom.xml`:

- **Spring Security** - Authentication & authorization
- **Spring Mail** - Email functionality
- **JWT (JJWT)** - JSON Web Token handling
- **BCrypt** - Password hashing
- **Commons Validator** - Email validation
- **MongoDB** - Data persistence

---

## 🗄️ Database Schema

### Users Collection
```json
{
  "_id": ObjectId,
  "name": "John Doe",
  "email": "john@example.com",
  "password": "$2a$10$...", // BCrypt hash
  "phone": "+1234567890",
  "emailVerified": false,
  "accountEnabled": true,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

### OTP Data Collection
```json
{
  "_id": ObjectId,
  "email": "john@example.com",
  "otp": "123456",
  "expiry": ISODate,
  "attempts": 0,
  "verified": false,
  "createdAt": ISODate
}
```

---

## 🔒 Security Best Practices

✅ **Implemented:**
- Passwords hashed with BCrypt (never stored in plain text)
- JWT tokens for stateless authentication
- CAPTCHA protection against bots
- Email OTP verification for account validation
- Password validation (minimum 8 characters)
- Email format validation
- Maximum OTP attempt limiting
- OTP expiration (5 minutes)
- Protected routes requiring authentication
- CORS configuration

⚠️ **Additional Recommendations:**
- Use HTTPS in production
- Store JWT secret in environment variables
- Implement rate limiting on auth endpoints
- Add logging and monitoring
- Regular security audits
- Keep dependencies updated
- Implement refresh tokens for long sessions
- Add account lockout after failed login attempts

---

## ⚙️ Running the Application

### Prerequisites
- Java 21+
- MongoDB running on `localhost:27017`
- Maven

### Build and Run
```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR file
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Access the Application
```
http://localhost:8080
```

---

## 🧪 Testing the API

### Using cURL

**Signup:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123",
    "phone": "+1234567890",
    "captchaToken": "TOKEN_FROM_RECAPTCHA"
  }'
```

**Verify OTP:**
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "otp": "123456"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "password": "SecurePass123", "captchaToken": "TOKEN"}'
```

---

## 🐛 Troubleshooting

### Email not sending?
- Verify Gmail credentials are correct
- Check if App Password is used (not Gmail password)
- Ensure 2FA is enabled on Gmail account
- Check MongoDB connection

### CAPTCHA validation failing?
- Verify reCAPTCHA keys are correct in application.properties
- Ensure token is being sent from frontend
- Check browser console for CAPTCHA errors

### OTP verification failing?
- Verify email is correct
- Check OTP hasn't expired (5 minutes)
- Ensure only 5 attempts made

### JWT token not working?
- Verify token is being sent in Authorization header
- Ensure Bearer prefix is included
- Check if token is expired

---

## 📝 Default Configuration Values

```properties
# OTP Configuration
otp.expiration=300000          # 5 minutes in milliseconds
otp.max-attempts=5             # Maximum OTP verification attempts

# JWT Configuration
jwt.expiration=86400000        # 24 hours in milliseconds

# Email Server
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review logs in console
3. Verify all placeholder values are replaced correctly
4. Check MongoDB is running and connected

---

## 📄 License

This authentication system is provided as-is for educational and production use.

---

**Last Updated:** 2024
**Version:** 1.0.0
