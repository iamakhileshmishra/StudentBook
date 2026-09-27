# StudentBook — Feature Roadmap (proposals)

| | |
|---|---|
| **Based on** | A review of every screen, API endpoint and table at commit `a5317fc`, after the bug-fix pass in `test&bug.md` |
| **Date** | 2026-09-28 |
| **Status** | Proposals only. **Nothing here is implemented.** |

**Priority**: 🔴 P0 needed before real schools rely on it · 🟠 P1 high value next · 🟡 P2 valuable later · 🔵 P3 long-term / beyond fees
**Effort**: S ≈ 1–2 days · M ≈ 3–7 days · L ≈ 2+ weeks (one developer)

---

## 1. What the app does today

| Area | Capabilities |
|---|---|
| **Public site** | Landing page (about, placeholder pricing, `mailto:` contact form), school sign-up with emailed User ID and temporary password, login, forgot password (email code) |
| **Auth & security** | JWT sessions with silent refresh and restore on reload, logout revocation, lockout, rate limits, first-login password change, emailed code to change the password, roles in the API (`SCHOOL_ADMIN`, `FEE_COLLECTOR`) |
| **Dashboard** | Today's and this month's collection, paid/partial/unpaid counts, class-wise progress with a per-class student list, recent payments |
| **Students** | List/search/filter by class, add (auto or manual admission number), view, edit, remove (marks LEFT), concession amount and note, staff-child flag |
| **Academic structure** | Classes and sections (add, rename, reorder, deactivate); one current academic year with editable dates |
| **Fee structure** | Fee categories (add, rename); monthly amount per class per category; copied forward each year |
| **Fee collection** | One student, one month at a time: dues with arrears and a late fee from tiered rules, cashier-editable concession and late fee, method (cash/UPI/cheque/bank), printable A4 receipt with amount in words |
| **Payments** | History with search and pagination, reprint receipt |
| **Settings** | School profile, due day and late-fee rules, classes and sections, change password, academic-year dates, CSV backup (ZIP), year-end promotion with arrears carry-forward |

**Built into the database but not used yet:**
- **Audit trail:** the `audit_logs` table isn't written by any feature.
- **Payment cancellation:** the `CANCELLED` status is never set.
- **Partial payments:** `remaining_balance` is always 0.
- **Student fields:** `photo_url` and `roll_number` are never shown or filled.
- **Staff child flag:** `is_staff_child` has no effect on fees.
- **Subscriptions:** `subscription_plan` and `subscription_expires_at` are unused.
- **Statuses and roles:** the `TRANSFERRED` student status and the `SUPER_ADMIN` role have no UI.

---

## 2. Top 10 to build next

| # | Feature | Why now | Priority | Effort |
|---|---|---|---|---|
| 1 | **Staff accounts** (add fee collectors, deactivate, reset password) | Roles are enforced by the API, but there's no way to create a second user | 🔴 P0 | M |
| 2 | **Cancel / void a payment** with reason and approval | Cashier mistakes can't be corrected today; the only way out is editing the DB | 🔴 P0 | M |
| 3 | **Audit log** (who changed fees, concessions, payments, settings) plus a viewer | Money software needs accountability; the table already exists | 🔴 P0 | M |
| 4 | **Defaulter report**: unpaid students by month/class with phone numbers, export to Excel/PDF | The most-asked-for report in any fee office | 🔴 P0 | S–M |
| 5 | **Collection reports**: day book / cash closing by date range, method and cashier | Needed for daily cash reconciliation | 🔴 P0 | M |
| 6 | **Student fee ledger**: every month paid/unpaid, receipts, arrears on the student page | Answers "what does this child owe?" in one place | 🟠 P1 | S–M |
| 7 | **Multi-month and partial payments** in one receipt | Parents often pay a quarter at once, or part of a month | 🟠 P1 | M–L |
| 8 | **Fee reminders** by SMS / WhatsApp / email (due date, overdue) | Directly increases collection | 🟠 P1 | M |
| 9 | **Online payments** (Razorpay/UPI link or QR) with automatic receipts | Removes cash handling; parents pay from home | 🟠 P1 | L |
| 10 | **Engineering baseline**: automated tests, CI, a clean `npm run build` (see §9) | Every change above is risky without it | 🔴 P0 | M |

---

## 3. Fee collection & payments

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Cancel / void payment** | Admin-only. Requires a reason, keeps the original receipt marked **CANCELLED**, and reopens the month as unpaid; optional second-person approval. Uses the existing `status` column | 🔴 P0 | M |
| **Multi-month collection** | Pick several months (or "up to March") in one transaction, one receipt listing each month; late fee per month | 🟠 P1 | M |
| **Partial payments & balances** | Accept less than the total and carry the balance to the next month; the dashboard's "partial" tile finally means something (`remaining_balance`) | 🟠 P1 | M |
| **Advance payments / credit** | Money paid ahead is held as credit and applied automatically to the next dues | 🟡 P2 | M |
| **Refunds** | For students who leave after paying ahead; refund receipt, method and reason | 🟡 P2 | M |
| **Cheque lifecycle** | Record cheque number, bank and date; mark cleared or **bounced** (re-opens dues, optional bounce charge) | 🟡 P2 | S–M |
| **Waiver / discount approvals** | A concession above a limit, or a late fee waived at the counter, requires admin approval and a reason (feeds the audit log) | 🟠 P1 | M |
| **Sequential receipt numbers** | Per-school, per-year gapless sequence (e.g. `NSNVPS/2026-27/000123`) instead of the random suffix; auditors expect this | 🟠 P1 | S |
| **Receipt improvements** | School logo, digital signature/stamp, "DUPLICATE" watermark on reprints, QR code to verify authenticity, email/WhatsApp the PDF to the parent | 🟠 P1 | M |
| **Receipt template settings** | Choose A4 / A5 / thermal-printer layout, parent+school copies, custom footer | 🟡 P2 | M |
| **Fee collection shortcuts** | Barcode/QR on ID cards to open a student instantly; recent students list; keyboard shortcuts for the counter | 🟡 P2 | S |

## 4. Fee structure

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Fee frequency per category** | Monthly, quarterly, half-yearly, annual, one-time (admission fee, annual charges); today everything is monthly | 🟠 P1 | L |
| **Optional fee heads per student** | Transport, hostel, computer lab or activities only for students who opt in; transport priced by route/stop | 🟠 P1 | M–L |
| **Per-student fee overrides** | Custom amount for one head for one student (e.g. special arrangements), with audit | 🟡 P2 | M |
| **Automatic discounts** | Sibling discount (link siblings), staff-child discount (the flag exists but is unused), merit/scholarship rules, % or flat | 🟠 P1 | M |
| **Deactivate / reorder categories in the UI** | The API has an `active` flag but no screen; reordering fee heads | 🟡 P2 | S |
| **Mid-year fee revision** | Change fees from a given month, keeping history (today an edit changes the whole year) | 🟡 P2 | M |
| **Fee structure copy & compare** | Copy one class's structure to others; compare with last year (+x% increase) | 🔵 P3 | S |

## 5. Students

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Bulk import / export** | Upload an Excel/CSV of students (with validation preview and error rows) and export the list; essential when onboarding a school with 500+ students | 🔴 P0 | M |
| **Student fee ledger & statement** | Month grid (paid/unpaid/partial), all receipts, arrears history, printable statement for parents | 🟠 P1 | S–M |
| **Sibling linking & family view** | Link siblings under one family/guardian: combined dues, one receipt, sibling discount | 🟡 P2 | M |
| **Photos & documents** | Student photo (`photo_url`), birth certificate, TC and other uploads, stored in object storage | 🟡 P2 | M |
| **Roll numbers & ID cards** | Assign roll numbers (`roll_number` exists), generate printable ID cards with QR | 🟡 P2 | S–M |
| **Transfer / re-admission** | Issue a transfer certificate (`TRANSFERRED` status), re-admit a LEFT student, and see LEFT/GRADUATED students in a separate list | 🟡 P2 | M |
| **Section change mid-year** | Move a student between sections/classes within the year, with history | 🟡 P2 | S |
| **Custom fields** | Blood group, Aadhaar (masked), category, house and similar, configurable per school | 🔵 P3 | M |

## 6. Reports & dashboard

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Defaulters report** | By month/range and class; amount due, months pending, phone; bulk "send reminder" | 🔴 P0 | S–M |
| **Day book / cash closing** | Collections for a date range, grouped by method and cashier; totals that match the cash drawer; print and lock the day | 🔴 P0 | M |
| **Fee head-wise report** | How much tuition, transport, exam fees etc. was collected (for accounts) | 🟠 P1 | S |
| **Outstanding & forecast** | Total receivable for the year, expected vs collected per month, collection-efficiency % | 🟠 P1 | M |
| **Trends** | Month-over-month and year-over-year charts, payment-method mix | 🟡 P2 | S–M |
| **Export everywhere** | Excel/PDF export on every table and report | 🟠 P1 | S |
| **Concession report** | All concessions and waivers given, by whom and why (with the audit log) | 🟡 P2 | S |

## 7. Communication

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Fee reminders** | Scheduled SMS / WhatsApp / email before the due date and after it's overdue; templates with student name, amount and pay link | 🟠 P1 | M |
| **Payment confirmations** | Automatic receipt PDF to the parent on every payment | 🟠 P1 | S (after reminders) |
| **Circulars / announcements** | Send a message to all parents, a class, or defaulters | 🟡 P2 | M |
| **Contact form backend** | The landing-page form currently opens `mailto:`; store enquiries and notify the sales inbox | 🟡 P2 | S |

## 8. Parents & online payments

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Online payment gateway** | Razorpay/PhonePe/UPI: payment link or QR per student, webhook marks the month paid and issues the receipt; reconciliation report | 🟠 P1 | L |
| **Parent portal (web)** | Parents sign in with phone + OTP to see dues, pay online and download receipts | 🟡 P2 | L |
| **Mobile app** | Parent and staff apps (React Native, sharing the API) | 🔵 P3 | L |

## 9. Platform, security & operations

| Feature | Details | Priority | Effort |
|---|---|---|---|
| **Staff user management** | Admin screen to invite staff (email with temp password, reusing sign-up), assign role, deactivate, reset password | 🔴 P0 | M |
| **Role-aware UI** | Hide admin-only buttons and pages for fee collectors (the API already blocks them; see test&bug.md SEC-05) | 🔴 P0 | S |
| **Audit log + viewer** | Record every create/update/delete on fees, concessions, payments, settings and users (who, when, before/after); filterable screen | 🔴 P0 | M |
| **Automated backups & restore** | Nightly DB backups off-site with retention; per-school restore/import of the CSV backup (today it's export-only) | 🔴 P0 | M |
| **Email-link verification + CAPTCHA** | Finish SEC-08 and SEC-09: verify the owner's email by link, and add Turnstile/reCAPTCHA to sign-up and forgot-password | 🟠 P1 | S–M |
| **Two-factor login for admins** | Optional TOTP or email code at login; list of active sessions with "sign out everywhere" | 🟡 P2 | M |
| **Super-admin console** | For you as the platform owner: list schools, usage, suspend/activate (`schools.is_active`), impersonate for support (audited) | 🟠 P1 | M–L |
| **Subscriptions & billing** | Real plans matching the pricing page (student limits), trial period, invoices, payment, reminders on expiry (`subscription_plan`, `subscription_expires_at`) | 🟠 P1 | L |
| **Multi-branch / school groups** | One owner managing several branches with a consolidated dashboard | 🔵 P3 | L |
| **School branding** | Logo upload (used on receipts and sidebar), colour theme | 🟡 P2 | S–M |
| **Localization** | Hindi (and regional languages) for the UI and receipts; Indian number formatting everywhere | 🟡 P2 | M |
| **Data privacy (DPDP Act)** | Export all of a school's data, delete school on request, retention policy, consent text | 🟡 P2 | M |
| **Notifications center** | In-app alerts: "10 students overdue", "backup failed", "subscription expiring" | 🔵 P3 | M |

### Engineering foundations (not user-visible, but they unblock everything above)

| Item | Details | Priority | Effort |
|---|---|---|---|
| **Automated tests** | There is no `backend/src/test` yet. Start with fee dues, late fees, promotion arrears, auth/lockout and tenant isolation (every scenario in test&bug.md is a ready-made test case) | 🔴 P0 | M |
| **Fix `npm run build`** | The frontend production build fails on ~15 unused-import TypeScript errors that pre-date this work | 🔴 P0 | S |
| **CI pipeline** | GitHub Actions: backend tests, frontend type-check/lint/build, Docker image on every PR | 🔴 P0 | S |
| **Production deployment** | HTTPS reverse proxy, prod compose/Kubernetes, managed Postgres, secrets from a vault, staging environment | 🔴 P0 | M |
| **Monitoring** | Error tracking (Sentry), uptime checks, structured logs, email-delivery monitoring | 🟠 P1 | S |
| **Performance** | The promotion preview runs one query per student per month, and the recent-payments list one per payment; batch them into a few SQL aggregates before schools with 1,000+ students arrive | 🟠 P1 | M |
| **Shared rate limiting** | Move the in-memory limiter to Redis/Bucket4j when running more than one backend instance | 🟡 P2 | S |
| **Consistent data access** | Enforce the tenant filter in one place (repository methods with `schoolId`, or a Hibernate filter) instead of per-query checks | 🟠 P1 | M |

## 10. Beyond fees (full school ERP, long-term)

These are the natural next modules if StudentBook grows from fee management into a full ERP. Each one is large and should be validated with schools first.

| Module | Scope | Priority | Effort |
|---|---|---|---|
| **Attendance** | Daily/period attendance per section, absentee SMS to parents, monthly reports | 🔵 P3 | L |
| **Exams & report cards** | Exam schedule, marks entry, grading scales, printable report cards | 🔵 P3 | L |
| **Timetable** | Class/teacher timetable builder with clash detection | 🔵 P3 | L |
| **Staff & payroll** | Staff records, salary structure, monthly payslips | 🔵 P3 | L |
| **Admissions / enquiries** | Online admission form, enquiry pipeline, admission test, convert to student | 🔵 P3 | M–L |
| **Transport** | Routes, stops, vehicles; transport fee by stop (links to §4) | 🔵 P3 | M |
| **Library / inventory** | Book issue/return, uniform/book sales with receipts | 🔵 P3 | M |

---

## 11. Suggested sequence

1. **Foundation (2–3 weeks)**
   - **Engineering:** tests, CI, a clean `npm run build` and a production deployment.
   - **Access:** staff user management and a role-aware UI.
   - **Accountability and safety:** the audit log and automated backups.
2. **Fee office essentials (3–4 weeks)**
   - **Corrections and receipts:** cancel/void payments with approvals, and sequential receipt numbers.
   - **Reports:** the defaulter report, day book, fee head-wise report and Excel/PDF export.
   - **Data:** the student ledger and bulk student import.
3. **Collect more, with less effort (4–6 weeks)**
   - **Payment flexibility:** multi-month and partial payments.
   - **Fee rules:** fee frequencies, optional heads and automatic discounts.
   - **Outreach and online payment:** reminders by SMS/WhatsApp/email, and the online payment gateway.
4. **Grow the platform**
   - **Platform operations:** the super-admin console, subscriptions and billing, and email verification with CAPTCHA.
   - **Parents and presentation:** the parent portal, branding and localization.
5. **Expand the ERP**
   - **Operations modules:** attendance and admissions.
   - **Academic modules:** exams and report cards.
   - Take on the rest of §10 as demand appears.
