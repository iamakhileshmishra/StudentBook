# StudentBook — QA Report: Security Bugs & Edge Cases

| | |
|---|---|
| **Scope** | Backend (Spring Boot, all modules) + frontend (React) at commit `d1b5d81` |
| **Method** | Code review of every controller/service/repository, plus live probes against the local Docker stack |
| **Date** | 2026-09-28 |
| **Code changes made** | None — findings only |

**Status legend** — ✅ *Confirmed*: reproduced against the running app. 🔍 *Code review*: found by reading the code; not reproduced (usually because it would need destructive actions or seeded students).

**Severity legend** — 🔴 Critical · 🟠 High · 🟡 Medium · 🔵 Low

---

## Summary

| ID | Severity | Area | Finding | Status |
|---|---|---|---|---|
| SEC-01 | 🔴 Critical | Multi-tenancy | Year-end promotion can modify **another school's students** | 🔍 |
| SEC-02 | 🔴 Critical | Auth / Config | JWT secret has a **hard-coded fallback** — forged tokens for any school if `JWT_SECRET` is unset | 🔍 |
| BUG-01 | 🟠 High | Auth (frontend) | **Silent token refresh never works** — users are logged out every ~15 min | ✅ |
| SEC-03 | 🟠 High | Auth | **Login lockout never triggers** — unlimited password guessing | ✅ |
| SEC-04 | 🟠 High | Auth | Sessions **not revoked** on password change/reset or logout (refresh token valid 7 days) | 🔍 |
| BUG-02 | 🟠 High | Fees | Promotion **multi-counts arrears** (previous dues added once per unpaid month, then again) | 🔍 |
| BUG-03 | 🟠 High | Fees | Fee collection **trusts client amounts** — negative/oversized concession & late fee, `null` → 500 | 🔍 |
| BUG-04 | 🟠 High | Fees | **Double payment** possible for the same student/month (race, no DB constraint) | 🔍 |
| BUG-05 | 🟠 High | Fees | Receipt number can exceed `VARCHAR(30)` → **fee collection fails** for some students | 🔍 |
| SEC-05 | 🟠 High | Authorization | **No role checks anywhere** — every logged-in user is effectively admin | 🔍 |
| SEC-06 | 🟡 Medium | Multi-tenancy | Fee amount update accepts **another school's class/category IDs** | 🔍 |
| SEC-07 | 🟡 Medium | Auth | **Username & email enumeration** via response timing and messages | ✅ |
| SEC-08 | 🟡 Medium | Abuse | Public endpoints have **no rate limiting / CAPTCHA** (sign-up spam, email bombing) | 🔍 |
| SEC-09 | 🟡 Medium | Registration | No email verification — anyone can **squat a school's email** | 🔍 |
| SEC-10 | 🟡 Medium | Auth | `must_change_password` **enforced only in the frontend** | 🔍 |
| SEC-11 | 🟡 Medium | Auth | Current-password checks in Settings/Remove Student have **no attempt limit** | 🔍 |
| SEC-12 | 🟡 Medium | Export | **CSV formula injection** in backup export | 🔍 |
| SEC-13 | 🟡 Medium | Info leak | Actuator health details & Swagger/OpenAPI are **public** | ✅ |
| BUG-06 | 🟡 Medium | Fees | Promotion counts **future & pre-admission months** as arrears, ignores concession | 🔍 |
| BUG-07 | 🟡 Medium | Students | Section not checked against class; inactive class/section accepted | 🔍 |
| BUG-08 | 🟡 Medium | Students | One huge numeric admission number **breaks auto-numbering** for the school | 🔍 |
| BUG-09 | 🟡 Medium | Students | Negative concession accepted on student create/update | 🔍 |
| BUG-10 | 🟡 Medium | Fees | Fees can be collected for students who have **LEFT/GRADUATED** | 🔍 |
| BUG-11 | 🟡 Medium | API | Invalid pagination / limits / not-found → **500 errors** | ✅ |
| BUG-12 | 🟡 Medium | Time | Server uses **UTC** for "today" — wrong date/late fee around IST midnight | 🔍 |
| BUG-13 | 🔵 Low | API | Unique-constraint violations surface as generic **500** instead of a clear message | 🔍 |
| BUG-14 | 🔵 Low | Fees | Deleted/inactive fee categories still charged, shown as "Fee" on dues/receipts | 🔍 |
| BUG-15 | 🔵 Low | Fees | Arrears-category fallback returns a fake UUID → later FK failure | 🔍 |
| BUG-16 | 🔵 Low | Settings | School profile / academic year / late-fee rules have almost no validation | 🔍 |
| SEC-14 | 🔵 Low | Transport | Refresh cookie `Secure=false` hard-coded, no `SameSite` | 🔍 |
| SEC-15 | 🔵 Low | Config | Dev credentials committed; Postgres & pgAdmin exposed on host ports | 🔍 |
| SEC-16 | 🔵 Low | Auth | Invalid/expired bearer token makes **public** endpoints return 401 | ✅ |

---

## 🔴 Critical

### SEC-01 — Year-end promotion can modify another school's students
- **Where:** `backend/.../settings/service/AcademicYearPromotionService.java:194`, `:225`
- **What:** `promoteStudents` loads each student with `studentRepository.findById(p.getStudentId())` and **never checks `schoolId`**. For `LEFT`/`GRADUATED` decisions it sets `student.status` on whatever student ID is sent. For promotions it creates an enrollment row with `school_id = <caller's school>` for that foreign student, using `nextClassId`/`nextSectionId`, which are also **not validated** (they can belong to another school).
- **Impact:** A school admin who obtains another school's student UUID can mark that student LEFT/GRADUATED, or attach them to their own school. UUIDs are hard to guess, which limits exploitation, but this is a direct cross-tenant **write**.
- **Repro (needs 2 schools with students):** log in as School A → `POST /api/settings/academic-years/promote` with a School B student ID and `decision: "LEFT"` → School B's student status becomes `LEFT`.
- **Expected:** Student IDs from other schools are rejected or ignored.
- **Fix:** Use `findByIdAndSchoolId`; validate that `nextClassId`/`nextSectionId` belong to the caller's school and that the section belongs to the class.

### SEC-02 — Hard-coded JWT secret fallback
- **Where:** `backend/src/main/resources/application.properties:26` → `app.jwt.secret=${JWT_SECRET:ThisIsADevSecretKey…}`
- **What:** If `JWT_SECRET` is missing in production, the app silently uses a secret that is **published in the repository**. The same is true for the dev secret in `docker-compose.yml`.
- **Impact:** Anyone can sign an `ACCESS` token with any `schoolId`/`sub`/`role` → full access to **every school's data**.
- **Fix:** No default value; fail startup if the secret is missing or shorter than 64 bytes. Rotate the secret if it has ever been deployed.

---

## 🟠 High

### BUG-01 — Silent token refresh never works (users logged out every ~15 min) ✅
- **Where:** `frontend/src/api/axios.ts:16` + `backend/.../shared/security/JwtAuthFilter.java:66`
- **What:** The axios request interceptor attaches the (expired) access token to **every** request, including `POST /api/auth/refresh`. `JwtAuthFilter` sees an invalid bearer token and returns **401 before the refresh endpoint runs**. The response interceptor then logs the user out.
- **Evidence:**
  ```
  refresh, no bearer:               200 {"accessToken": ...}
  refresh, invalid/expired bearer:  401 {"error":"Unauthorized","message":"Token is invalid or expired"}
  ```
- **Impact:** Every user is kicked to the login page when the 15-minute access token expires, which can lose unsaved work (e.g. mid fee collection).
- **Fix:** Don't send `Authorization` on `/auth/*` requests, and/or make the filter ignore the bearer token on `permitAll` paths (see SEC-16).
- **Test:** Log in, wait 15 minutes (or shorten `app.jwt.access-token-expiry-ms`), click anything → you should stay logged in.

### SEC-03 — Login lockout never triggers ✅
- **Where:** `backend/.../auth/service/AuthService.java:32` (`@Transactional`), `:48`
- **What:** `handleFailedLogin` increments `failed_attempts`, then `BusinessException` is thrown and **rolls back the transaction**, including the increment.
- **Evidence:** 6 wrong passwords for `gvps.admin` → `failed_attempts=0 locked_until=null`.
- **Impact:** Unlimited online password guessing against any User ID (User IDs are predictable: initials + `.admin`).
- **Fix:** `@Transactional(noRollbackFor = BusinessException.class)` (as done in `OtpService` callers), or save the counter in a `REQUIRES_NEW` transaction. Add per-IP throttling too (SEC-08).

### SEC-04 — Sessions not revoked on password change, reset, or logout
- **Where:** `AuthService.refreshAccessToken` (`AuthService.java:95`), `AuthController.logout` (`AuthController.java:100`)
- **What:** Tokens are stateless and never invalidated. After a **password reset** (the typical response to a compromised account), the attacker's refresh token keeps minting new access tokens for **7 days**. `logout` only clears the cookie in the current browser; a stolen refresh token still works.
- **Fix:** Add a `token_version` (or `password_changed_at`) column to users, embed it in tokens, and reject tokens whose version is stale. Bump it on password change/reset/logout-all.
- **Test:** Log in on browser A → reset password from browser B → browser A should be logged out on its next refresh.

### BUG-02 — Promotion multi-counts previous-year arrears
- **Where:** `AcademicYearPromotionService.java:165` and `:218`; `FeeCollectionService.getDues` (adds `previousDues` into `subtotal`)
- **What:** `getDues()` includes the enrollment's `previousDues` in `subtotal` **for every month**. Promotion sums `subtotal` over every unpaid month (so previous dues are counted once per unpaid month, up to 12×) and then adds `oldEnrollment.getPreviousDues()` **again**.
- **Example:** A student carries ₹1,000 of old arrears and has 3 unpaid months of ₹500. Expected carry-forward ₹2,500; computed ₹(500+1000)×3 + 1000 = **₹5,500**.
- **Fix:** Compute arrears from the fee structure only, and add `previousDues` exactly once (only if still unpaid).

### BUG-03 — Fee collection trusts client-supplied amounts
- **Where:** `FeeCollectionService.java:201`; `CollectFeeRequest` has **no validation annotations**
- **What:** `grandTotal = subtotal − request.concessionAmount + request.lateFee`, taken directly from the client:
  - `concessionAmount`/`lateFee` = `null` → **NullPointerException → 500**.
  - Negative `lateFee` or concession larger than the fee → negative total → DB `CHECK` violation → 500 (or, near the boundary, a ₹0 receipt).
  - The late fee the server calculated (`expectedLateFee`) is ignored; a user can silently waive it via the API.
  - `paymentMethod` outside the allowed list → DB check violation → 500. `month`/`year` unvalidated (future months collectable; month 13 → 500 in `getDues`, `FeeCollectionService.java:107`).
- **Fix:** Bean validation (`@NotNull`, `@DecimalMin("0")`, `@Min(1) @Max(12)`, enum for method). Recompute the late fee server-side, cap concession at the subtotal, and record any override with a reason.

### BUG-04 — Duplicate payment for the same student & month
- **Where:** `FeeCollectionService.java:161` (check-then-insert); no unique constraint on `payment_transactions`
- **What:** The "already paid" check and the insert aren't atomic. A double-click or two cashiers collecting at the same time → **two CONFIRMED payments** for the same month. Dashboard totals double-count.
- **Fix:** Partial unique index `ON payment_transactions (student_id, collection_year, collection_month) WHERE status = 'CONFIRMED'`, plus disabling the Pay button while the request is in flight.
- **Test:** Fire two `POST /api/fees/collection` requests at the same time for one student/month → expect exactly one success.

### BUG-05 — Receipt number overflows `VARCHAR(30)`
- **Where:** `FeeCollectionService.java:180`; column `payment_transactions.receipt_number VARCHAR(30)`
- **What:** Format is `SHORT-YYYY-MMDD-ADMNO-XXXX` = `len(short) + len(admNo) + 16`. The short name can be up to 50 characters (Settings) or 10 (sign-up), and the admission number up to 20.
  - `NSNVPS` + a 9-character admission number = 31 characters → **insert fails, fee cannot be collected** (500).
- **Fix:** Widen the column (e.g. 64) and/or cap short name and admission number lengths; better, use a per-school sequential receipt counter.
- **Test:** Student with admission number `ADM2026001`, school short name `SUNRISE` → collect fee → expect success.

### SEC-05 — No role-based authorization
- **Where:** No `@PreAuthorize`/`hasRole` anywhere; `SecurityConfig` only requires "authenticated".
- **What:** The schema defines `SUPER_ADMIN | SCHOOL_ADMIN | FEE_COLLECTOR`, but roles are never checked. Once staff accounts exist, a fee collector could change fee structures, promote students, download the full backup, and so on.
- **Fix:** Define a permission matrix and enforce it with `@PreAuthorize` on each endpoint (see ROADMAP "Role-based access control").

---

## 🟡 Medium

### SEC-06 — Fee amount update accepts foreign class/category IDs
- **Where:** `FeeStructureService.java:199` (`updateFeeAmount`), `:217`
- **What:** `classId`/`feeCategoryId` aren't checked against the caller's school. An upsert creates a `monthly_fee_structures` row owned by School A that points at School B's class/category. The response then returns School B's **category name** (`categoryRepository.findById` without a school filter).
- **Fix:** Validate both IDs with `findByIdAndSchoolId` before the upsert.

### SEC-07 — Username/email enumeration ✅
- **Evidence (local):**
  ```
  login  nosuchuser → 0.013s     login  gvps.admin (wrong pw) → 0.280s
  forgot unknown email → 0.016s  forgot known email → 0.314s
  ```
  The deactivated/locked messages (`AuthService.java:40`, `:44`) also reveal that an account exists.
- **Impact:** Lets an attacker confirm which User IDs and emails exist before brute-forcing (SEC-03) or phishing.
- **Fix:** Run a dummy BCrypt compare for unknown users; send email asynchronously so both forgot-password paths take the same time; use one generic error message.

### SEC-08 — No rate limiting on public endpoints
- **Endpoints:** `/api/auth/login`, `/register`, `/forgot-password`, `/forgot-password/verify`, `/forgot-password/reset`
- **Impact:**
  - **Sign-up:** can create unlimited schools, and each sign-up sends an email to any address, so the platform can be used as a spam relay.
  - **Forgot-password:** limited to 1 email/min per account, but unlimited over time and across accounts.
  - **OTP guessing:** 5 wrong tries per code and 1 new code per minute works out to about 7,200 guesses/day per account, against 10⁶ possible codes.
- **Fix:** Per-IP and per-account rate limits (e.g. Bucket4j), a CAPTCHA on sign-up and forgot-password, and a daily cap on codes per account.

### SEC-09 — No email verification at sign-up
- **What:** The school email is unique, so registering someone else's address **blocks the real owner** from ever signing up with it, and sends them an unsolicited email.
- **Fix:** Create the school as `PENDING` until an emailed link/code is confirmed; expire unverified registrations.

### SEC-10 — First-login password change enforced only in the frontend
- **What:** A user with `must_change_password = true` can call every API directly using the token from `/login`.
- **Fix:** Add a claim in the access token, and have the filter reject all endpoints except `change-password`/`logout` while it is set.

### SEC-11 — No attempt limit on current-password checks
- **Where:** `/api/settings/change-password/request-otp`, `/api/settings/change-password`, `/api/students/{id}/remove`
- **What:** Someone who hijacks an active session can brute-force the account password through these endpoints (no counter, no lockout), then change it.
- **Fix:** Share the login failure counter/lockout, or throttle per user.

### SEC-12 — CSV formula injection in backup export
- **Where:** `BackupService.java:106` (only quotes `,` `"` and newline)
- **What:** A student named `=HYPERLINK("http://evil","Click")` (or starting with `+ - @`) is exported as-is. When opened in Excel or Sheets, the cell runs as a formula.
- **Fix:** Prefix cells starting with `= + - @ \t \r` with `'`.

### SEC-13 — Public actuator details and API docs ✅
- `GET /actuator/health` (anonymous) → `{"db":{"details":{"database":"PostgreSQL",...}},"diskSpace":{...}}` (`show-details=always`, `application.properties:58`).
- `GET /v3/api-docs` and `/swagger-ui` → 200 anonymously: the full API map is public.
- **Fix:** `show-details=when-authorized`; disable springdoc in the prod profile, or put it behind authentication.

### BUG-06 — Promotion arrears include future and pre-admission months
- **Where:** `AcademicYearPromotionService.java:147–171`
- **What:** Every unpaid month from academic-year start to end is counted, including:
  - **Months that haven't happened yet**, if promotion runs before March.
  - **Months before the student's enrollment date**, for mid-year admissions.
  - **The full fee even for students with a concession.** Arrears use `subtotal`, which ignores the student's concession.
- **Fix:** Only count months from `max(yearStart, enrollmentDate)` to `min(yearEnd, today)`, net of concession.

### BUG-07 — Student class/section consistency
- **Where:** `StudentService.java:188`
- **What:** The section is validated against the school but **not against the chosen class** (a student can be put in "Std I – section of Std V"). Inactive (deleted) classes and sections are also accepted.
- **Fix:** Require `section.classId == classId`, and both `active`.

### BUG-08 — Admission-number auto-generation can be broken by one value
- **Where:** `StudentRepository.java:20` → `MAX(CAST(admission_number AS INTEGER))`
- **What:** Manually entering an all-digit admission number larger than 2,147,483,647 (e.g. `99999999999`, allowed by `VARCHAR(20)`) makes the cast overflow. **Every later "auto" admission number fails** for that school (500).
- **Fix:** Cast to `BIGINT`/`NUMERIC`, and cap manual admission numbers (length/pattern) in the DTO.

### BUG-09 — Negative concession accepted
- **Where:** `StudentCreateRequest.java:38`, `UpdateStudentRequest.java:39`: no `@DecimalMin("0")`
- **What:** A negative concession *increases* the fee. A concession larger than the monthly fee drives totals negative (→ BUG-03 errors).
- **Fix:** `@DecimalMin("0")`; warn if it exceeds the class's monthly total.

### BUG-10 — Fees collectable for LEFT/GRADUATED students
- **Where:** `FeeCollectionService.collectFee`: requires an enrollment row but ignores `student.status`/`enrollment.status`.
- **Fix:** Block collection (or require confirmation) unless the enrollment is `ACTIVE`; still allow collecting arrears explicitly.

### BUG-11 — Bad input returns 500 instead of 400/404 ✅
| Request | Result |
|---|---|
| `GET /api/payments/history?size=0` | 500 |
| `GET /api/payments/history?page=-1` | 500 |
| `GET /api/payments/history?size=1000000` | 200: unbounded page size (heavy query; DoS risk) |
| `GET /api/dashboard/recent?limit=-5` | 500 |
| `GET /api/payments/receipt/NOPE` | 500 (`IllegalArgumentException`, `PaymentHistoryService.java:34`) |
| `GET /api/fees/collection/dues?month=13…` | 500 (`YearMonth.of`) |

- **Fix:** Clamp `page ≥ 0`, `1 ≤ size ≤ 100`, `1 ≤ limit ≤ 50`; map `IllegalArgumentException`/`IllegalStateException` to 400 and "not found" to 404 in `GlobalExceptionHandler`.

### BUG-12 — Server time zone is UTC
- **What:**
  - **Wrong date near midnight India time.** `LocalDate.now()` runs in the container's UTC, so between 00:00 and 05:30 IST the receipt date, the "today's collection" total and the days-late count all use **yesterday's date**.
  - **Wrong academic year.** Sign-up uses `Asia/Kolkata` for its academic-year calculation, so the two parts of the app disagree.
- **Fix:** Store the time zone per school (default `Asia/Kolkata`), and use `ZoneId` consistently.

---

## 🔵 Low

### BUG-13 — Constraint violations → generic 500
Duplicate fee-category name (create/rename), duplicate class name, duplicate academic-year name during promotion, school email changed to one already used by another school, or two people signing up with the same email at the same moment: all return "An unexpected error occurred". **Fix:** Handle `DataIntegrityViolationException` → 409 with a readable message, and pre-check where practical.

### BUG-14 — Inactive fee categories still charged
`getDues`/`collectFee` sum **all** fee-structure rows for the class, but category names come only from *active* categories. A deactivated category is still charged and shows up as "Fee" on dues and receipts. **Fix:** Filter the structure rows by active category.

### BUG-15 — Arrears category fallback UUID
`FeeCollectionService.java:287`: on any error it returns `00000000-…-0001`, which isn't a real category → the later line-item insert fails with an FK error, far from the real cause. **Fix:** Let the exception surface.

### BUG-16 — Weak validation in Settings
- **School profile (`SettingsService.java:70`):**
  - **Unvalidated fields:** only `name` is checked, so email, phone and pincode formats aren't. The short name can be up to 50 characters of anything, which feeds into BUG-05 and the backup file name.
  - **Clearing the school email** leaves the admin unable to receive password-reset and change-password codes.
- **Academic year (`SettingsService.java:201`):** there's no `@Valid`. An end date before the start date → DB check → 500. Overlap with other years isn't checked.
- **Late-fee rules:**
  - **Bad ranges accepted:** negative amounts, and overlapping or unordered day ranges.
  - **Invalid rules fail silently:** if the stored JSON can't be read, `getDues` quietly applies ₹0 late fee.

### SEC-14 — Refresh cookie attributes
`AuthController.java:110`: `setSecure(false)` is hard-coded, so the 7-day refresh token can travel over plain HTTP. There's no `SameSite` attribute either. **Fix:** `Secure` from config (true in prod), `SameSite=Strict`, and consider cookie path `/api/auth/refresh`.

### SEC-15 — Committed dev credentials and exposed services
`docker-compose.yml`:
- **Committed credentials:** the Postgres password `schoolerp_dev`, the dev JWT secret, and the pgAdmin login `admin/admin` are all in the repository.
- **Exposed ports:** Postgres is published on host port `5432`, and pgAdmin on `5050` when that profile is enabled.

That's fine for local dev, but make sure none of it reaches a server. **Fix:** Separate prod compose/env, and don't publish the DB port.

### SEC-16 — Bad bearer token breaks public endpoints ✅
With an invalid/expired `Authorization` header, `/api/auth/login` and `/api/auth/refresh` return **401** (`JwtAuthFilter.java:66`), even though they're `permitAll`. This is the root cause of BUG-01. **Fix:** On invalid tokens, clear the context and continue the chain; let Spring Security decide based on the endpoint's rules.

---

## ✔ Checked and looks OK
- **SQL injection:** all `JdbcTemplate` queries use `?` parameters; search terms are bound, not concatenated.
- **XSS:**
  - **Page content:** React escapes it, and there's no `dangerouslySetInnerHTML`.
  - **Receipt printing:** it writes the already-rendered (escaped) DOM, and email templates escape user input with `HtmlUtils.htmlEscape`.
- **Tenant isolation in reads:** students, dashboard, payments history, receipts, backup and academic structure all filter by `TenantContext.getSchoolId()` (exceptions: SEC-01, SEC-06).
- **Access token storage:** kept in memory, not `localStorage`; the refresh token is in an `HttpOnly` cookie.
- **OTP design:**
  - **Codes:** hashed, single-use, 10-minute expiry, 5-attempt limit, and only the newest code works.
  - **Scope:** tied to a purpose (reset vs change); attempts persist correctly (`noRollbackFor`).
  - **Enumeration:** forgot-password gives the same response for known and unknown emails (apart from timing, SEC-07).
- **Temporary password:** never returned by the API, only emailed; generated with `SecureRandom`.

---

## Test Cases to Run (manual / automated)

### Authentication & sessions
- [ ] Wrong password 5× → account locked 5 minutes; 6th attempt shows the lock message (fails today, SEC-03)
- [ ] Stay idle 16 minutes, then click a page → still logged in via silent refresh (fails today, BUG-01)
- [ ] Refresh the browser (F5) → session survives (fails today: token kept only in memory, no refresh on load)
- [ ] Reset password in browser B → browser A session ends (fails today, SEC-04)
- [ ] Logout, then replay the old refresh cookie → rejected (fails today, SEC-04)
- [ ] User ID login is case-insensitive (`GVPS.ADMIN`) ✅
- [ ] Deactivated user cannot log in or refresh
- [ ] `must_change_password` user calling `/api/students` directly → blocked (fails today, SEC-10)
- [ ] Forged token signed with the default secret is rejected in prod config (SEC-02)

### Registration
- [ ] Required-field, pincode (6 digits), phone and email validation messages appear per field ✅
- [ ] Same email twice (any case) → friendly error ✅; two simultaneous submits → no 500
- [ ] Short name blank → initials used; short name with symbols → rejected ✅
- [ ] School names that give < 3 initials (`"St X"`, `"DPS"`), non-English names (`"विद्या मंदिर"`), names that are only numbers/symbols → sensible User ID
- [ ] SMTP down → registration rolls back, no orphan school ✅
- [ ] New school: dashboard, fee structure and students pages work with 0 classes/students ✅

### Forgot password / change password (OTP)
- [ ] Unknown email → same message as known ✅; timing roughly equal (fails today, SEC-07)
- [ ] Code expires after 10 minutes; used code can't be reused ✅
- [ ] 5 wrong codes → locked; the correct code is then rejected too ✅
- [ ] Resend within 60 seconds → no new email ✅; after 60 seconds → the old code is invalid
- [ ] Reset code can't be used for a Settings change and vice versa ✅
- [ ] Settings change: wrong current password, same password, < 8 characters, mismatch → inline errors ✅
- [ ] School with no email → clear message when changing password
- [ ] Reset unlocks a locked account ✅

### Fees
- [ ] Double-click "Collect" → exactly one payment (BUG-04)
- [ ] Concession > total, negative late fee, empty concession → 400 with a message (BUG-03)
- [ ] Month 13 / year 1900 / future month → 400
- [ ] School short name `SUNRISE`, admission number `ADM2026001` → receipt saved (BUG-05)
- [ ] Deactivated fee category is not charged (BUG-14)
- [ ] Late fee exactly on the due day, 1 day late, beyond the highest rule tier, due day 31 in February
- [ ] LEFT student → collection blocked (BUG-10)
- [ ] Payment at 00:30 IST → receipt date is today in IST (BUG-12)

### Students
- [ ] Section from a different class → rejected (BUG-07)
- [ ] Admission number `99999999999`, then add a student with an auto number (BUG-08)
- [ ] Negative concession → rejected (BUG-09)
- [ ] Duplicate admission number (manual and concurrent) → friendly error
- [ ] Leading zeros `007` stored as `7`; `000` handling
- [ ] Very long names/addresses → validation, not a 500

### Year-end promotion
- [ ] Student with old arrears + 3 unpaid months → correct carry-forward (BUG-02)
- [ ] Mid-year admission → no arrears for months before joining (BUG-06)
- [ ] Promotion run in January → future months not counted (BUG-06)
- [ ] Payload with another school's student/class ID → rejected (SEC-01)
- [ ] Double-submit the promotion → second run blocked
- [ ] Duplicate new academic-year name → friendly error

### Multi-tenancy (two schools, A and B)
- [ ] With A's token, call every `{id}` endpoint using B's IDs (student, class, section, category, receipt, promotion, fee update) → 404/400 every time (fails today: SEC-01, SEC-06)
- [ ] Backup ZIP for A contains no rows from B

### API robustness
- [ ] `page=-1`, `size=0`, `size=100000`, `limit=-5` → 400 or clamped (BUG-11)
- [ ] Unknown receipt → 404 (BUG-11)
- [ ] Malformed JSON / wrong types → 400, not 500
- [ ] Unauthenticated access to `/actuator/health`, `/v3/api-docs` in prod → no details (SEC-13)

### Export
- [ ] Student named `=1+1` → backup CSV cell shows the text, not `2` (SEC-12)

---

## Suggested fix order
1. **SEC-02, SEC-01, BUG-01, SEC-03.** These are small changes with the biggest security or user impact.
2. **BUG-03, BUG-04, BUG-05, BUG-02.** These fix money correctness.
3. **SEC-04, SEC-05, SEC-06, SEC-10.** These cover sessions and authorization.
4. **SEC-07/08/09/11/12/13, BUG-06–12.** Hardening, then the edge cases.
5. **The 🔵 Low items.** Handle these as housekeeping.
