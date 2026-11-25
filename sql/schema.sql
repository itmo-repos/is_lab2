-- PostgreSQL

DROP TABLE IF EXISTS is_lab2_import_operation;
DROP TABLE IF EXISTS is_lab2_account;

DROP TABLE IF EXISTS is_lab1_lab_work CASCADE;
DROP TABLE IF EXISTS is_lab1_person CASCADE;
DROP TABLE IF EXISTS is_lab1_location CASCADE;
DROP TABLE IF EXISTS is_lab1_discipline CASCADE;
DROP TABLE IF EXISTS is_lab1_coordinates CASCADE;

CREATE TABLE is_lab1_coordinates (
    id BIGSERIAL PRIMARY KEY,
    x  BIGINT           NOT NULL CHECK (x <= 577),
    y  DOUBLE PRECISION NOT NULL
);

CREATE TABLE is_lab1_discipline (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    practice_hours INTEGER NOT NULL,
    labs_count     INTEGER NOT NULL
);

CREATE TABLE is_lab1_location (
    id   BIGSERIAL PRIMARY KEY,
    x    INTEGER NOT NULL,
    y    BIGINT  NOT NULL,
    name VARCHAR(255) CHECK (name IS NULL OR length(trim(name)) > 0)
);

CREATE TABLE is_lab1_person (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    eye_color   VARCHAR(16) NOT NULL CHECK (eye_color IN ('GREEN', 'BLUE', 'WHITE', 'BROWN')),
    hair_color  VARCHAR(16) CHECK (hair_color IN ('GREEN', 'BLUE', 'WHITE', 'BROWN')),
    location_id BIGINT REFERENCES is_lab1_location (id) ON DELETE SET NULL,
    height      BIGINT NOT NULL CHECK (height > 0),
    nationality VARCHAR(32) NOT NULL CHECK (nationality IN ('UNITED_KINGDOM', 'USA', 'VATICAN'))
);

CREATE TABLE is_lab1_lab_work (
    id                        SERIAL PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL CHECK (length(trim(name)) > 0),
    coordinates_id            BIGINT NOT NULL REFERENCES is_lab1_coordinates (id),
    creation_date             TIMESTAMPTZ NOT NULL DEFAULT now(),
    description               TEXT NOT NULL,
    difficulty                VARCHAR(16) NOT NULL CHECK (difficulty IN ('HARD', 'VERY_HARD', 'IMPOSSIBLE', 'INSANE')),
    discipline_id             BIGINT REFERENCES is_lab1_discipline (id) ON DELETE SET NULL,
    minimal_point             REAL    CHECK (minimal_point > 0),
    personal_qualities_minimum BIGINT CHECK (personal_qualities_minimum > 0),
    author_id                 BIGINT NOT NULL REFERENCES is_lab1_person (id)
);

CREATE INDEX is_lab1_idx_lab_work_discipline ON is_lab1_lab_work (discipline_id);
CREATE INDEX is_lab1_idx_lab_work_author ON is_lab1_lab_work (author_id);

CREATE TABLE is_lab2_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE CHECK (username ~ '^[a-z0-9_.-]{3,32}$'),
    salt BYTEA NOT NULL CHECK (octet_length(salt) = 16),
    password_hash BYTEA NOT NULL CHECK (octet_length(password_hash) = 32),
    role VARCHAR(5) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE is_lab2_import_operation (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES is_lab2_account (id),
    status VARCHAR(9) NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    added_count INTEGER,
    CHECK (
        (status = 'SUCCEEDED' AND added_count IS NOT NULL AND added_count >= 0)
        OR (status = 'FAILED' AND added_count IS NULL)
    )
);

CREATE INDEX is_lab2_idx_import_account ON is_lab2_import_operation (account_id, id);
