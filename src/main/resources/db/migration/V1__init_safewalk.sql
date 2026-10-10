-- ============================================================================
-- SafeWalk baseline migration (Phase 1)
-- Establishes the initial schema and proves Flyway <-> MySQL connectivity.
-- Future feature migrations (users, contacts, trips, sos, reports, help data)
-- will be added as V2, V3, ... in later phases.
-- ============================================================================

-- Internal bookkeeping table so the baseline migration is meaningful and
-- Flyway has a concrete schema change to apply on a fresh database.
CREATE TABLE IF NOT EXISTS safewalk_schema_info (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    info_key    VARCHAR(100) NOT NULL,
    info_value  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_safewalk_schema_info_key (info_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO safewalk_schema_info (info_key, info_value)
VALUES ('schema_version', 'V1'),
       ('app', 'SafeWalkBackend')
ON DUPLICATE KEY UPDATE info_value = VALUES(info_value);
