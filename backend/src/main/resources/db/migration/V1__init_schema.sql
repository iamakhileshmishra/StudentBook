-- ═══════════════════════════════════════════════════════════════════════════
-- V1 — Complete Schema Initialization
-- School Fee Management System (Multi-Tenant)
-- ═══════════════════════════════════════════════════════════════════════════

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ───────────────────────────────────────────────────────────────────────────
-- SCHOOLS (Tenant Table)
-- One row per school. school_id is the tenant key on all other tables.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE schools (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(200) NOT NULL,
    short_name              VARCHAR(50),
    address_line1           VARCHAR(255),
    address_line2           VARCHAR(255),
    city                    VARCHAR(100),
    state                   VARCHAR(100),
    pincode                 VARCHAR(10),
    phone                   VARCHAR(15),
    email                   VARCHAR(100),
    website                 VARCHAR(200),
    logo_url                VARCHAR(500),
    is_active               BOOLEAN     NOT NULL DEFAULT TRUE,
    subscription_plan       VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    subscription_expires_at TIMESTAMP,
    created_at              TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP   NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE schools IS 'Tenant table. Every data table references this via school_id.';

-- ───────────────────────────────────────────────────────────────────────────
-- USERS
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools (id),
    username        VARCHAR(50) NOT NULL,
    email           VARCHAR(100),
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(30) NOT NULL,
    -- Roles: SUPER_ADMIN | SCHOOL_ADMIN | FEE_COLLECTOR
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    failed_attempts INTEGER     NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_school_username UNIQUE (school_id, username)
);

COMMENT ON COLUMN users.role IS 'SUPER_ADMIN=platform owner, SCHOOL_ADMIN=school manager, FEE_COLLECTOR=counter staff';

-- ───────────────────────────────────────────────────────────────────────────
-- ACADEMIC YEARS
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE academic_years (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID        NOT NULL REFERENCES schools (id),
    name        VARCHAR(20) NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    is_current  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_academic_years_school_name UNIQUE (school_id, name),
    CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date)
);

-- Ensure only one current academic year per school
CREATE UNIQUE INDEX uq_one_current_year_per_school
    ON academic_years (school_id)
    WHERE is_current = TRUE;

-- ───────────────────────────────────────────────────────────────────────────
-- CLASSES
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE classes (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID        NOT NULL REFERENCES schools (id),
    name          VARCHAR(50) NOT NULL,
    numeric_order INTEGER     NOT NULL,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_classes_school_order UNIQUE (school_id, numeric_order),
    CONSTRAINT uq_classes_school_name  UNIQUE (school_id, name)
);

-- ───────────────────────────────────────────────────────────────────────────
-- SECTIONS
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE sections (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID        NOT NULL REFERENCES schools (id),
    class_id  UUID        NOT NULL REFERENCES classes (id),
    name      VARCHAR(10) NOT NULL,
    is_active BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sections_class_name UNIQUE (class_id, name)
);

-- ───────────────────────────────────────────────────────────────────────────
-- STUDENTS
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE students (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id             UUID        NOT NULL REFERENCES schools (id),
    admission_number      VARCHAR(20) NOT NULL,

    first_name            VARCHAR(50) NOT NULL,
    last_name             VARCHAR(50) NOT NULL,
    date_of_birth         DATE,
    gender                VARCHAR(10),

    -- Parent / Guardian
    father_name           VARCHAR(100),
    mother_name           VARCHAR(100),
    guardian_name         VARCHAR(100),
    primary_contact_phone VARCHAR(15) NOT NULL,
    alternate_phone       VARCHAR(15),
    email                 VARCHAR(100),
    address               TEXT,

    -- Concession
    is_staff_child        BOOLEAN      NOT NULL DEFAULT FALSE,
    concession_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    concession_note       TEXT,

    -- Status
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE | INACTIVE | TRANSFERRED | LEFT

    photo_url             VARCHAR(500),
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by            UUID REFERENCES users (id),

    CONSTRAINT uq_students_school_admission UNIQUE (school_id, admission_number),
    CONSTRAINT chk_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'LEFT')),
    CONSTRAINT chk_concession_positive CHECK (concession_amount >= 0)
);

-- ───────────────────────────────────────────────────────────────────────────
-- STUDENT ENROLLMENTS
-- Links a student to a class/section for a given academic year.
-- One row per student per academic year.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE student_enrollments (
    id               UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID      NOT NULL REFERENCES schools (id),
    student_id       UUID      NOT NULL REFERENCES students (id),
    academic_year_id UUID      NOT NULL REFERENCES academic_years (id),
    class_id         UUID      NOT NULL REFERENCES classes (id),
    section_id       UUID      NOT NULL REFERENCES sections (id),
    roll_number      VARCHAR(10),
    enrollment_date  DATE      NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_enrollment_student_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'LEFT', 'TRANSFERRED'))
);

-- ───────────────────────────────────────────────────────────────────────────
-- FEE CATEGORIES
-- e.g. Tuition Fee, Transport Fee, Examination Fee, etc.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE fee_categories (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID        NOT NULL REFERENCES schools (id),
    name          VARCHAR(100) NOT NULL,
    display_order INTEGER     NOT NULL DEFAULT 99,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fee_categories_school_name UNIQUE (school_id, name)
);

-- ───────────────────────────────────────────────────────────────────────────
-- MONTHLY FEE STRUCTURES
-- Default fee amounts per class per academic year.
-- This is the TEMPLATE that pre-fills the collection form.
-- The fee collector can override any amount to 0 or another value.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE monthly_fee_structures (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID          NOT NULL REFERENCES schools (id),
    academic_year_id UUID          NOT NULL REFERENCES academic_years (id),
    class_id         UUID          NOT NULL REFERENCES classes (id),
    fee_category_id  UUID          NOT NULL REFERENCES fee_categories (id),
    default_amount   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_by       UUID REFERENCES users (id),
    CONSTRAINT uq_fee_structure_year_class_cat UNIQUE (school_id, academic_year_id, class_id, fee_category_id),
    CONSTRAINT chk_fee_structure_amount CHECK (default_amount >= 0)
);

-- ───────────────────────────────────────────────────────────────────────────
-- PAYMENT TRANSACTIONS
-- One record per fee collection session (one visit to the fee counter).
-- IMMUTABLE after creation — only status can change (to CANCELLED).
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE payment_transactions (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID          NOT NULL REFERENCES schools (id),
    receipt_number      VARCHAR(30)   NOT NULL,

    student_id          UUID          NOT NULL REFERENCES students (id),
    enrollment_id       UUID          NOT NULL REFERENCES student_enrollments (id),
    academic_year_id    UUID          NOT NULL REFERENCES academic_years (id),

    -- Which month is this payment for?
    collection_month    INTEGER       NOT NULL CHECK (collection_month BETWEEN 1 AND 12),
    collection_year     INTEGER       NOT NULL,

    -- Financial snapshot (all calculated at time of creation, stored as snapshot)
    previous_balance    NUMERIC(10, 2) NOT NULL DEFAULT 0,
    subtotal            NUMERIC(10, 2) NOT NULL,
    concession_applied  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    late_fee            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    grand_total         NUMERIC(10, 2) NOT NULL,
    amount_paid         NUMERIC(10, 2) NOT NULL,
    remaining_balance   NUMERIC(10, 2) NOT NULL,

    -- Payment details
    payment_method      VARCHAR(20)   NOT NULL,
    -- CASH | UPI | CHEQUE | BANK_TRANSFER | OTHER
    payment_reference   VARCHAR(100),
    payment_date        DATE          NOT NULL,
    remarks             TEXT,
    amount_in_words     VARCHAR(255),

    -- Status
    status              VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED',
    -- CONFIRMED | CANCELLED
    collected_by        UUID          NOT NULL REFERENCES users (id),
    confirmed_at        TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Cancellation info (Admin only)
    cancelled_by        UUID REFERENCES users (id),
    cancelled_at        TIMESTAMP,
    cancellation_reason TEXT,

    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_payment_school_receipt UNIQUE (school_id, receipt_number),
    CONSTRAINT chk_payment_method CHECK (
        payment_method IN ('CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'OTHER')
    ),
    CONSTRAINT chk_payment_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_amounts_positive CHECK (
        subtotal >= 0 AND grand_total >= 0 AND amount_paid >= 0
    )
);

-- ───────────────────────────────────────────────────────────────────────────
-- PAYMENT LINE ITEMS
-- Individual fee component amounts within a payment session.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE payment_line_items (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          NOT NULL REFERENCES schools (id),
    transaction_id  UUID          NOT NULL REFERENCES payment_transactions (id),
    fee_category_id UUID          NOT NULL REFERENCES fee_categories (id),
    amount_charged  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_line_item_amount CHECK (amount_charged >= 0)
);

-- ───────────────────────────────────────────────────────────────────────────
-- AUDIT LOGS
-- Immutable log of all significant actions.
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID REFERENCES schools (id),
    -- NULL for platform-level events (SUPER_ADMIN actions)
    user_id     UUID REFERENCES users (id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   UUID,
    description TEXT,
    old_values  JSONB,
    new_values  JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════════════════════
-- INDEXES for performance
-- ═══════════════════════════════════════════════════════════════════════════

-- Students
CREATE INDEX idx_students_school_id          ON students (school_id);
CREATE INDEX idx_students_school_status       ON students (school_id, status);
CREATE INDEX idx_students_admission           ON students (school_id, admission_number);

-- Enrollments
CREATE INDEX idx_enrollments_student_year     ON student_enrollments (student_id, academic_year_id);
CREATE INDEX idx_enrollments_school_year      ON student_enrollments (school_id, academic_year_id);
CREATE INDEX idx_enrollments_class            ON student_enrollments (class_id);

-- Fee structures
CREATE INDEX idx_fee_structure_year_class     ON monthly_fee_structures (academic_year_id, class_id);

-- Payments
CREATE INDEX idx_payments_school_id           ON payment_transactions (school_id);
CREATE INDEX idx_payments_student_id          ON payment_transactions (student_id);
CREATE INDEX idx_payments_school_month        ON payment_transactions (school_id, collection_year, collection_month);
CREATE INDEX idx_payments_school_date         ON payment_transactions (school_id, payment_date);
CREATE INDEX idx_payments_collected_by        ON payment_transactions (collected_by);

-- Line items
CREATE INDEX idx_line_items_transaction       ON payment_line_items (transaction_id);

-- Audit
CREATE INDEX idx_audit_school_created         ON audit_logs (school_id, created_at DESC);
CREATE INDEX idx_audit_entity                 ON audit_logs (entity_type, entity_id);
