-- V3: support importing the bundled SafeWalk datasets. Non-destructive.
-- Help-centre "sectors" values in the source dataset are up to ~860 characters.
ALTER TABLE help_locations MODIFY COLUMN support_sectors VARCHAR(1000) NULL;

-- Annual, state-level crimes-against-women statistics (historical, not live).
CREATE TABLE IF NOT EXISTS women_crime_statistics (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    state          VARCHAR(80)  NOT NULL,
    region_context VARCHAR(160) NULL,
    stat_year      INT          NOT NULL,
    category       VARCHAR(60)  NOT NULL,
    case_count     INT          NULL,
    source         VARCHAR(255) NOT NULL,
    source_key     VARCHAR(200) NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL,
    updated_at     TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_women_crime_statistics_source_key UNIQUE (source_key),
    INDEX idx_women_crime_statistics_state_year (state, stat_year)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
