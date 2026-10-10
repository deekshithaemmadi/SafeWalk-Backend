
-- SafeWalk V2: Create tables required by JPA entities

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(190) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE emergency_contacts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    relation VARCHAR(80) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_emergency_contacts_user (user_id),
    CONSTRAINT fk_emergency_contacts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE community_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    moderation_note VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_reports_user (user_id),
    INDEX idx_reports_status (status),
    INDEX idx_reports_created_at (created_at),
    CONSTRAINT fk_community_reports_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE help_locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(160),
    district VARCHAR(120),
    locality VARCHAR(190),
    address VARCHAR(500),
    latitude DOUBLE,
    longitude DOUBLE,
    phone VARCHAR(40),
    support_sectors VARCHAR(500),
    tier VARCHAR(80),
    source_year VARCHAR(20),
    source VARCHAR(255),
    source_key VARCHAR(400) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_help_locations_source_key UNIQUE (source_key),
    INDEX idx_help_locations_type (type),
    INDEX idx_help_locations_district (district),
    INDEX idx_help_locations_locality (locality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sos_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message VARCHAR(500),
    share_location BOOLEAN NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    accuracy DOUBLE,
    requested_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_sos_events_user (user_id),
    CONSTRAINT fk_sos_events_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE trips (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    origin_label VARCHAR(255),
    origin_latitude DOUBLE,
    origin_longitude DOUBLE,
    destination_label VARCHAR(255),
    destination_latitude DOUBLE,
    destination_longitude DOUBLE,
    last_latitude DOUBLE,
    last_longitude DOUBLE,
    started_at TIMESTAMP(6) NULL,
    ended_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_trips_user (user_id),
    INDEX idx_trips_status (status),
    CONSTRAINT fk_trips_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
