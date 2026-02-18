# Project Summary

## Phase 2: Full-Stack Implementation (Complete)

**Project Title:** User Registration and Login System with Advanced Security, Admin Dashboard, and Frontend Integration

The User Registration and Login System is a comprehensive full-stack web application designed to manage user authentication, authorization, and access control in a secure, scalable, and production-ready manner. Building upon the Phase 1 backend foundation, this phase introduces JWT-based stateless authentication, email verification, password reset via email, role-based access control (RBAC), rate limiting, audit logging, admin-level user management with statistics, theme personalization, and a fully redesigned modern dark-mode frontend. The system now represents a complete, deployment-ready authentication platform.

---

## Objective

The primary objective of this phase is to evolve the basic backend registration and login system into a fully functional, secure, and user-friendly full-stack application. The key goals include:

- **Implement JWT-based authentication** to replace session-based auth with stateless, token-based access control suitable for modern web applications and APIs.
- **Secure all user passwords** using industry-standard BCrypt hashing to ensure data at rest is protected against brute-force and rainbow table attacks.
- **Introduce role-based access control (RBAC)** so that administrative routes and data are protected and only accessible by users with the ADMIN role.
- **Build email verification and password reset flows** using token-based email links, allowing users to verify their identity and recover accounts securely.
- **Implement audit logging** to track every critical action (logins, registrations, password resets, status changes) with timestamps and IP addresses for compliance and debugging.
- **Add rate limiting** to protect authentication endpoints from brute-force attacks by throttling excessive requests per IP.
- **Design and develop a modern frontend** using HTML, CSS, and JavaScript with a professional dark-mode SaaS aesthetic, replacing the initial generic UI.
- **Build a fully functional admin dashboard** with real-time statistics, user management (search, filter, suspend, activate, delete), and audit log viewing.
- **Enable theme personalization** allowing users to switch between dark and light modes with server-side persistence.

---

## Scope of Work (SoW)

### 1. JWT-Based Authentication
The system implements stateless authentication using JSON Web Tokens (JWT). Upon successful login, a token is generated containing the user's username and role, signed with a configurable HMAC-SHA256 secret key. The token is sent to the client and must be included in the `Authorization` header of all subsequent API requests. A custom `JwtAuthenticationFilter` intercepts every request, validates the token, extracts user details, and sets the Spring Security authentication context. Token expiration is configurable via `application.properties`.

### 2. Password Security with BCrypt
All user passwords are hashed using the BCrypt algorithm via Spring Security's `BCryptPasswordEncoder` before being stored in the database. Raw passwords are never stored. During login, the submitted password is compared against the stored hash using a constant-time comparison to prevent timing attacks.

### 3. Role-Based Access Control (RBAC)
Two roles are defined: `USER` and `ADMIN`. New users are assigned the `USER` role by default upon registration. Administrative endpoints such as user status management (`PUT /api/users/{id}/status`), user deletion (`DELETE /api/users/{id}`), audit log viewing (`GET /api/audit/logs`), and statistics retrieval (`GET /api/statistics`) are protected using Spring Security's `@PreAuthorize("hasAuthority('ADMIN')")` annotation. The frontend dynamically shows or hides admin-only sections based on the decoded JWT role.

### 4. Email Verification
Upon registration, the system generates a unique UUID-based verification token, stores it in the `email_verification_tokens` database table with a 24-hour expiration, and sends a styled HTML verification email to the user. Clicking the link in the email triggers the `GET /api/auth/verify-email?token=xxx` endpoint, which validates the token, marks the user's email as verified, and records the action in the audit log.

### 5. Forgot Password and Password Reset
A dedicated "Forgot Password" page allows users to submit their registered email address. The backend generates a unique password reset token (valid for 1 hour), stores it in the `password_reset_tokens` table, and sends a styled HTML email containing a reset link. The reset link directs the user to the `/reset-password` page, where they enter and confirm a new password. The backend validates the token (checking expiration and usage status), updates the password hash, marks the token as used, and logs the action. The response is deliberately ambiguous ("If that email exists, a reset link has been sent") to prevent email enumeration attacks.

### 6. Rate Limiting
Login and registration endpoints are protected by the Bucket4j rate-limiting library. Each IP address is limited to 5 requests per minute. The `RateLimitService` uses a `ConcurrentHashMap` of token buckets, creating a new bucket per unique IP. Requests exceeding the limit receive an HTTP 429 (Too Many Requests) response, preventing brute-force credential attacks.

### 7. Audit Logging
Every security-relevant action is recorded in the `audit_logs` database table. The `AuditService` logs the following events with timestamps, IP addresses, user references, and human-readable details:
- `USER_REGISTERED` — when a new account is created
- `LOGIN_SUCCESS` — when a user logs in successfully
- `LOGIN_FAILED` — when a login attempt fails (wrong password, suspended account, etc.)
- `EMAIL_VERIFIED` — when a user verifies their email
- `PASSWORD_RESET_REQUESTED` — when a forgot-password email is sent
- `PASSWORD_RESET_COMPLETED` — when a user successfully resets their password

The `AuditController` exposes `GET /api/audit/logs` (admin-only) to retrieve audit logs in reverse chronological order, displayed in the admin dashboard with color-coded action badges.

### 8. User Management (Admin)
Admin users can manage all registered users through the dashboard:
- **Search and filter** users by username/email (text search), role (USER/ADMIN), and account status (ACTIVE/SUSPENDED/DISABLED) using JPA Specification-based dynamic queries.
- **Update user status** — suspend or reactivate accounts via `PUT /api/users/{id}/status`.
- **Delete users** — permanently remove users and all associated records (verification tokens, reset tokens, audit logs) via `DELETE /api/users/{id}`, with proper foreign-key cascade handling.
- **Paginated responses** — the user list API supports server-side pagination with configurable page size.

### 9. Admin Statistics Dashboard
A dedicated `StatisticsController` provides real-time system metrics visible only to admins:
- Total registered users
- Active users (not suspended or disabled)
- Suspended users
- Users registered today, this week, and this month
- Email-verified vs. unverified user counts

These statistics are displayed as animated stat cards on the dashboard home view.

### 10. User Profile Management
Authenticated users can manage their own profiles through the `/api/profile` endpoints:
- **View profile** — retrieve username, email, role, account status, email verification status, theme preference, registration date, and last login timestamp.
- **Update profile** — change username or email (with duplicate-check validation; changing email resets verification status).
- **Change password** — requires current password verification before accepting a new password (minimum 6 characters).
- **Theme preference** — switch between DARK and LIGHT themes, persisted server-side and applied on page load.

### 11. Frontend Redesign
The entire frontend was redesigned from a basic gradient-and-centered-form layout to a professional, modern dark-mode SaaS aesthetic:

#### Authentication Pages (Login, Register, Reset Password, Forgot Password)
- **Split-screen layout**: Left branding panel with animated geometric shapes, gradient logo, descriptive text, and feature highlights. Right panel with the actual form.
- **Unique accent colors**: Teal for login, indigo for register, amber for reset password.
- **Enhanced form elements**: Icon-prefixed input fields, focus glow effects, animated submit buttons with shimmer effect, and contextual feedback messages.
- **Specialized indicators**: Password strength meter on the registration page, real-time password match indicator on the reset password page, step-by-step instructions on the forgot password page.
- **Inter-page navigation**: "Forgot password?" link on login, "Create account" link on login/reset, "Sign in" link on register/forgot-password pages.

#### Dashboard Page
- **Sidebar navigation layout** with branding header, nav links (Dashboard, Users, Audit Logs), and a user profile card showing the current user's avatar, username, and role badge.
- **Three distinct views** accessible via sidebar navigation:
  - **Dashboard view**: Welcome banner, animated stat cards (admin only), and quick-action cards for navigating to other views.
  - **Users view**: Search bar, role/status filter dropdowns, and a full user management table with role and status badges, and admin action buttons (Suspend/Activate/Delete).
  - **Audit Logs view** (admin only): Table of recent system events with color-coded action badges, timestamps, user references, details, and IP addresses.
- **Smooth view switching**: Sidebar highlights the active view, page title updates dynamically, and each view fades in with a CSS animation.
- **Theme toggle**: Dark/light mode switch in the top bar, with preference persisted server-side.
- **Toast notifications**: Non-intrusive feedback messages for all user actions (status updates, deletions, theme changes, errors).
- **Responsive design**: Sidebar collapses on smaller screens.

### 12. Security Configuration
Spring Security is configured with the following rules:
- **CSRF disabled** (stateless REST API).
- **Default form login and HTTP Basic disabled**.
- **Stateless session management** (no server-side sessions; JWT handles all auth).
- **Public routes**: `/api/auth/**`, static resources, frontend pages (`/login`, `/register`, `/dashboard`, `/forgot-password`, `/reset-password`).
- **Protected routes**: `/api/users/**`, `/api/profile/**`, `/api/statistics`, `/api/audit/**` — all require a valid JWT.
- **Admin-only routes**: Protected at the method level using `@PreAuthorize`.
- **JWT filter** runs before Spring's `UsernamePasswordAuthenticationFilter` in the filter chain.

### 13. Database Design and Integration
MySQL is used as the relational database with Hibernate ORM for automatic schema management (`ddl-auto=update`). The database schema includes:

| Table                        | Purpose                                                    |
|-----------------------------|------------------------------------------------------------|
| `users`                     | Core user data: credentials, role, status, email verification, theme, timestamps |
| `audit_logs`                | Action tracking with user FK, action type, IP, details, timestamps |
| `email_verification_tokens` | UUID tokens for email verification with user FK, expiration, used flag |
| `password_reset_tokens`     | UUID tokens for password reset with user FK, expiration, used flag |

All token tables have proper foreign key relationships to the `users` table, and cascade deletion is handled in the application layer.

### 14. REST API Development
The complete API surface includes:

| Method | Endpoint                        | Access      | Description                              |
|--------|--------------------------------|-------------|------------------------------------------|
| POST   | `/api/auth/register`           | Public      | Register new user, send verification email |
| POST   | `/api/auth/login`              | Public      | Authenticate user, return JWT             |
| GET    | `/api/auth/verify-email`       | Public      | Verify email via token                    |
| POST   | `/api/auth/forgot-password`    | Public      | Send password reset email                 |
| POST   | `/api/auth/reset-password`     | Public      | Reset password using token                |
| GET    | `/api/users`                   | Authenticated | List users with search, filter, paginate |
| PUT    | `/api/users/{id}/status`       | Admin       | Update user account status                |
| DELETE | `/api/users/{id}`              | Admin       | Delete user and related data              |
| GET    | `/api/profile`                 | Authenticated | Get current user's profile               |
| PUT    | `/api/profile`                 | Authenticated | Update username or email                 |
| POST   | `/api/profile/change-password` | Authenticated | Change password (requires current)       |
| PUT    | `/api/profile/theme`           | Authenticated | Update theme preference                  |
| GET    | `/api/statistics`              | Admin       | Get system statistics                     |
| GET    | `/api/audit/logs`              | Admin       | Get audit logs                            |

---

## Technology Stack

| Component             | Technology                                                           |
|----------------------|----------------------------------------------------------------------|
| Language              | Java 17                                                              |
| Framework             | Spring Boot 3.2.2                                                    |
| Security              | Spring Security 6, JWT (jjwt 0.11.5), BCryptPasswordEncoder         |
| Database              | MySQL 8.x                                                            |
| ORM                   | Hibernate / Spring Data JPA                                          |
| Email                 | Spring Boot Starter Mail (JavaMailSender, MIME, SMTP/Gmail)          |
| Rate Limiting         | Bucket4j 8.1.0                                                       |
| Template Engine       | Thymeleaf (for serving HTML pages)                                    |
| Frontend              | HTML5, CSS3 (custom), Vanilla JavaScript (Fetch API)                 |
| Typography            | Google Fonts (Space Grotesk, Inter)                                   |
| Build Tool            | Apache Maven                                                          |
| API Testing           | Postman, Browser DevTools                                             |
| Version Control       | Git                                                                   |

---

## Methodology

### 1. Requirement Analysis
The system requirements were derived from the Phase 1 next-steps: JWT authentication, password encryption, frontend integration, and role-based access control. Additional requirements for email verification, password reset, audit logging, rate limiting, and admin management were identified based on industry best practices for production-grade authentication systems.

### 2. Security-First Architecture Design
A layered architecture was designed with security as the primary concern:
- **Controller layer**: Handles HTTP requests, input validation, and response formatting.
- **Service layer**: Contains business logic for auditing, email dispatch, rate limiting, and user details resolution.
- **Repository layer**: Provides database access via Spring Data JPA interfaces with custom query methods.
- **Security layer**: JWT token generation/validation, authentication filter, and Spring Security configuration.
- **Model layer**: JPA entities with proper relationships, enums for type safety (Role, AccountStatus, Theme), and transient computed fields.
- **DTO layer**: Data Transfer Objects for API request/response to avoid exposing entity internals.

### 3. Iterative Development
Development followed an iterative process:
- Phase 2a: Implemented JWT authentication, BCrypt hashing, and security configuration.
- Phase 2b: Added email verification, password reset, and email service with styled HTML templates.
- Phase 2c: Built rate limiting, audit logging, and admin management endpoints.
- Phase 2d: Developed the statistics and user profile APIs.
- Phase 2e: Redesigned the entire frontend with dark-mode SaaS aesthetic, split-screen auth pages, and sidebar dashboard.
- Phase 2f: Implemented functional sidebar navigation with view switching, forgot-password page, and final polish.

### 4. Database Integration
MySQL was configured as the production database. Hibernate's `ddl-auto=update` strategy was used for automatic schema evolution during development. Entity relationships were defined using JPA annotations (`@ManyToOne`, `@JoinColumn`) with lazy loading for performance. Foreign key constraints are handled at the application level to ensure clean user deletion cascades through token and audit log tables.

### 5. Frontend Design and Implementation
The frontend was designed with modern web design principles:
- Dark-mode-first approach with CSS custom properties for theme switching.
- Split-screen authentication layout inspired by modern SaaS platforms.
- Dashboard with sidebar navigation and smooth view transitions.
- Responsive design with mobile breakpoints.
- All API interactions use the JavaScript Fetch API with JWT Bearer tokens.

### 6. API Testing
All REST APIs were tested using Postman for correctness, error handling, and edge cases. Frontend integration was tested by running the full application and exercising all user flows in the browser.

---

## Project Architecture

```
user-auth-system/
├── src/main/java/com/example/demo/
│   ├── UserAuthSystemApplication.java          # Main Spring Boot entry point
│   ├── config/
│   │   ├── SecurityConfig.java                 # Spring Security + JWT filter chain
│   │   └── PasswordConfig.java                 # BCryptPasswordEncoder bean
│   ├── controller/
│   │   ├── AuthController.java                 # Register, login, verify, forgot/reset password
│   │   ├── UserController.java                 # User listing, status update, deletion (admin)
│   │   ├── UserProfileController.java          # Profile view/update, change password, theme
│   │   ├── AuditController.java                # Audit log retrieval (admin)
│   │   ├── StatisticsController.java           # System statistics (admin)
│   │   ├── ViewController.java                 # Thymeleaf template routing
│   │   └── TestController.java                 # Health check endpoint
│   ├── dto/
│   │   ├── RegisterRequest.java                # Registration payload
│   │   ├── LoginRequest.java                   # Login payload
│   │   ├── AuthResponse.java                   # JWT token response
│   │   ├── ForgotPasswordRequest.java          # Forgot password payload
│   │   ├── ResetPasswordRequest.java           # Reset password payload
│   │   ├── UserDTO.java                        # User data transfer object
│   │   └── UserSearchRequest.java              # Search/filter parameters
│   ├── model/
│   │   ├── User.java                           # Core user entity
│   │   ├── Role.java                           # USER, ADMIN enum
│   │   ├── AccountStatus.java                  # ACTIVE, SUSPENDED, DISABLED enum
│   │   ├── Theme.java                          # DARK, LIGHT enum
│   │   ├── AuditLog.java                       # Audit record entity
│   │   ├── EmailVerificationToken.java         # Email token entity
│   │   └── PasswordResetToken.java             # Password reset token entity
│   ├── repository/
│   │   ├── UserRepository.java                 # User CRUD + search queries
│   │   ├── AuditLogRepository.java             # Audit log queries
│   │   ├── EmailVerificationTokenRepository.java
│   │   └── PasswordResetTokenRepository.java
│   ├── security/
│   │   ├── JwtUtil.java                        # Token generation + validation
│   │   └── JwtAuthenticationFilter.java        # Request interceptor
│   └── service/
│       ├── AuditService.java                   # Action logging
│       ├── CustomUserDetailsService.java       # Spring Security UserDetails
│       ├── EmailService.java                   # Verification + reset emails
│       └── RateLimitService.java               # IP-based throttling
├── src/main/resources/
│   ├── application.properties                  # DB, JWT, email config
│   └── templates/
│       ├── login.html                          # Login page (split-screen, teal accent)
│       ├── register.html                       # Registration page (indigo accent)
│       ├── forgot-password.html                # Forgot password page (teal accent)
│       ├── reset-password.html                 # Reset password page (amber accent)
│       └── dashboard.html                      # Admin dashboard (sidebar + 3 views)
└── pom.xml                                     # Maven dependencies
```

---

## Challenges Faced

### 1. JWT Integration with Spring Security
Integrating JWT with Spring Security 6 required careful configuration of the filter chain. The `JwtAuthenticationFilter` had to be positioned before the default `UsernamePasswordAuthenticationFilter`, and the security configuration had to disable default form login and HTTP Basic while allowing public access to authentication endpoints. Incorrect filter ordering initially caused all requests to be rejected.

### 2. Database Migration and Schema Evolution
Adding new fields (`emailVerified`, `accountStatus`, `theme`, `lastLogin`, `profilePicturePath`) and new tables (`audit_logs`, `email_verification_tokens`, `password_reset_tokens`) to an existing database required careful handling. Issues were encountered with default datetime values and null column constraints in MySQL, which were resolved through proper entity defaults and Hibernate configuration.

### 3. Foreign Key Constraint Handling During User Deletion
Deleting a user required first removing all related records from `email_verification_tokens`, `password_reset_tokens`, and `audit_logs` tables to avoid foreign key constraint violations. This cascade logic was implemented at the application layer in the `deleteUser` method.

### 4. Email Service Configuration
Configuring Gmail SMTP for email dispatch required proper App Password setup (not regular passwords), correct TLS settings, and handling of cases where the email service is not configured. The system gracefully handles email sending failures and provides meaningful error messages.

### 5. Rate Limiting Concurrency
Implementing per-IP rate limiting required thread-safe data structures (`ConcurrentHashMap`) to handle concurrent request processing. The Bucket4j library provided a robust token-bucket implementation that integrates cleanly with the existing service architecture.

### 6. Frontend-Backend Integration
Ensuring the frontend JavaScript correctly handles JWT token storage, inclusion in API request headers, token decoding for UI state (showing admin sections), and proper redirection on 401/403 errors required careful coordination between client and server.

### 7. CORS Configuration
Cross-origin requests between the Thymeleaf-served frontend pages and the REST API endpoints required explicit CORS configuration on all controllers, allowing both `localhost:3000` (development) and `localhost:8080` (production) origins.

### 8. Password Reset Flow Security
The password reset flow required careful security considerations: using UUID tokens instead of sequential IDs, setting token expiration (1 hour), marking tokens as "used" after single use to prevent replay attacks, and providing ambiguous responses to prevent email enumeration.

---

## Results / Findings

### 1. Successful Full-Stack Execution
The complete application runs without runtime errors, serving both the REST API and the frontend pages from a single Spring Boot instance on port 8080.

### 2. Secure Authentication System
- JWT tokens are correctly generated, validated, and expired.
- Passwords are securely hashed using BCrypt (verified via database inspection — all stored passwords are BCrypt hashes).
- Rate limiting successfully blocks excessive login attempts (verified via Postman: 6th request within 1 minute returns HTTP 429).
- Token-based email verification and password reset work end-to-end.

### 3. Role-Based Access Control Enforced
- Regular users can only access their own profile and the user listing.
- Admin-only endpoints (statistics, audit logs, user management actions) correctly return HTTP 403 for non-admin users.
- The frontend dynamically hides admin-only UI elements for regular users.

### 4. Comprehensive Audit Trail
Every security-relevant action is logged with timestamps, IP addresses, and user references. The audit log view in the admin dashboard provides complete visibility into system activity.

### 5. Modern, Professional Frontend
The redesigned frontend provides a premium, polished user experience:
- Split-screen authentication pages with animated branding panels.
- Functional dashboard with sidebar navigation and three distinct views.
- Dark/light theme toggle with server-side persistence.
- Responsive layouts that work across screen sizes.

### 6. Accurate Database Operations
All CRUD operations work correctly:
- User registration creates records with proper defaults.
- Token tables correctly store and expire verification/reset tokens.
- Audit logs accurately record all tracked actions.
- User deletion properly cascades through all related tables.

### 7. Verified API Responses
All 14 API endpoints return correct HTTP status codes and response payloads:
- `200 OK` for successful operations.
- `400 Bad Request` for validation errors (duplicate username/email, weak password, expired token).
- `401 Unauthorized` for missing or invalid JWT.
- `403 Forbidden` for insufficient role permissions.
- `404 Not Found` for non-existent resources.
- `429 Too Many Requests` for rate-limited clients.
- `500 Internal Server Error` for unexpected failures (with meaningful error messages).

---

## Screenshots

- **Figure 1:** Login page with split-screen layout — teal accent branding panel on the left, sign-in form with "Forgot password?" link on the right

- **Figure 2:** Registration page with indigo accent branding panel and password strength indicator

- **Figure 3:** Forgot Password page with step-by-step recovery instructions

- **Figure 4:** Reset Password page with amber accent and real-time password match indicator

- **Figure 5:** Admin Dashboard — sidebar navigation, welcome banner, stat cards showing Total Users, Active Users, New This Week, and Verified Emails

- **Figure 6:** Users view — search bar, role/status filters, and user management table with role and status badges, Suspend/Activate/Delete action buttons

- **Figure 7:** Audit Logs view — chronological table of system events with color-coded action badges (login, register, reset, verify, fail)

- **Figure 8:** MySQL database — `users` table showing stored records with BCrypt-hashed passwords, roles, and account statuses

- **Figure 9:** MySQL database — `audit_logs` table showing tracked actions with timestamps and IP addresses

- **Figure 10:** Postman — Login API returning JWT token (HTTP 200 OK)

- **Figure 11:** Postman — Registration API creating new user (HTTP 200 OK)

- **Figure 12:** Spring Boot project structure showing layered architecture with controllers, services, repositories, models, DTOs, and security components

---

## Conclusion

The User Registration and Login System has been successfully evolved from a basic backend prototype into a comprehensive, production-grade full-stack authentication platform. The system now includes all essential security features — JWT authentication, BCrypt hashing, RBAC, email verification, password reset, rate limiting, and audit logging — alongside a modern, professional frontend that provides an excellent user experience. The layered architecture ensures maintainability and scalability, while the comprehensive API surface supports both the integrated frontend and potential future integrations with external applications.

---

## Future Scope

- **OAuth2 / Social Login**: Integration with Google, GitHub, or Microsoft for single sign-on (SSO)
- **Two-Factor Authentication (2FA)**: TOTP-based second factor using apps like Google Authenticator
- **Account Recovery Questions**: Additional account recovery mechanism beyond email
- **API Key Management**: Allow users to generate and manage API keys for programmatic access
- **User Activity Analytics**: Dashboard charts showing login trends, registration patterns, and geographic distribution
- **Password Policy Enforcement**: Configurable rules for password complexity, history, and rotation
- **Session Management**: View and revoke active sessions across devices
- **Containerization**: Docker and Docker Compose setup for easy deployment
- **CI/CD Pipeline**: Automated testing and deployment using GitHub Actions or Jenkins
