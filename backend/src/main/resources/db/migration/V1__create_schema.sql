CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE job_applications (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    company    VARCHAR(150) NOT NULL,
    role_title VARCHAR(150) NOT NULL,
    location   VARCHAR(150),
    job_url    VARCHAR(500),
    status     VARCHAR(20)  NOT NULL,
    applied_on DATE,
    notes      TEXT,
    version    BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_applications_user_updated (user_id, updated_at),
    INDEX idx_applications_user_status (user_id, status)
);

CREATE TABLE status_changes (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT       NOT NULL,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20)  NOT NULL,
    note           VARCHAR(500),
    changed_at     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_changes_application FOREIGN KEY (application_id) REFERENCES job_applications (id) ON DELETE CASCADE,
    INDEX idx_changes_application (application_id, changed_at)
);
