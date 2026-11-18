-- V2 was merged empty by mistake (PR #20) and is intentionally left as it is:
-- a merged migration is never edited. The users table is therefore created here, in V3.
-- Identity module: users who can log in.
-- Passwords are never stored, only a BCrypt hash.
CREATE TABLE users (
                       id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
                       username      varchar(50)  NOT NULL,
                       password_hash varchar(100) NOT NULL,
                       full_name     varchar(120) NOT NULL,
                       role          varchar(20)  NOT NULL,
                       enabled       boolean      NOT NULL DEFAULT true,
                       created_at    timestamptz  NOT NULL DEFAULT now(),
                       updated_at    timestamptz  NOT NULL DEFAULT now(),
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT ck_users_role CHECK (role IN ('EMPLOYEE', 'SUPERVISOR'))
);