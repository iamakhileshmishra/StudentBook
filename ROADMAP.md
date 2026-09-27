# StudentBook Roadmap

Tracks completed milestones and the backlog of upcoming features and fixes.
Move items to **Done** (with the commit hash) as they land.

## Done

| Milestone | Summary | Commit |
|---|---|---|
| M0 | Project foundation: schema, JWT auth, frontend skeleton | `a67da25` |
| M2 + M3 | Academic structure (years, classes, sections) and student management | `f174ac3` |
| M4 | Fee collection, receipts, payment history, settings UI | `66bfbe0` |
| — | Dynamic late fee rules | `70b92fb` |
| — | Fee structure redesign, auto-inheritance of structures into new years | `93d8f33` |
| — | Create and rename fee categories | `206049e` |

## Backlog

### Bugs

- [ ] **Account lockout never triggers after failed logins**
  - Where: `AuthService.login()` → `handleFailedLogin()` in `backend/src/main/java/com/schoolerp/modules/auth/service/`
  - Problem: `login()` is `@Transactional`. `handleFailedLogin` increments `failed_attempts`, but the
    `BusinessException` thrown straight after rolls the transaction back, so the counter stays at 0
    and the 5-attempt / 5-minute lock is never applied.
  - Fix: persist the failed attempt in its own transaction (`@Transactional(propagation = REQUIRES_NEW)`
    on a separate bean method), or use `@Transactional(noRollbackFor = BusinessException.class)`.
  - Verify: 5 wrong passwords → `failed_attempts = 5`, `locked_until` set, 6th attempt returns the
    "temporarily locked" message.

### Hardening

- [ ] **Enforce tenant isolation at the repository layer.** Some lookups use `findById` and then check
  `schoolId` by hand (for example `FeeStructureService.updateCategory`). Prefer `findByIdAndSchoolId`
  queries or a Hibernate `@Filter`, so a single missed check can't expose another school's data.
- [ ] **Role-based access control.** Only `SCHOOL_ADMIN` exists today. Add roles such as accountant
  and clerk, and restrict endpoints with `@PreAuthorize`.
- [ ] **Automated tests.** There is no `backend/src/test` yet. Start with fee dues calculation,
  late fees, year-end promotion, auth lockout, and tenant isolation.

### Housekeeping

- [ ] Remove the committed `backend_logs*.txt` files from the repo and ignore them in `.gitignore`.
- [ ] Put controllers in one consistent place (some sit in the module root, some in `controller/`).
- [ ] Add a `.gitattributes` to normalise line endings (Git warns about LF→CRLF conversions).
- [ ] Add a dev-only way to reset the admin password (the README credentials stop working once the
  password is changed in Settings, because the Postgres volume persists).
