# SafeWalk-Backend

Spring Boot 4.1 / Java 17 REST API for SafeWalk, a women's night-travel safety web app. The frontend is a separate repository.
Full endpoint reference with examples and required frontend changes: [docs/API.md](docs/API.md).

## Run locally

Requirements: JDK 17, MySQL 8 (or Docker), Maven 3.9+ (or `./mvnw`).

```bash
docker run -d --name safewalk-mysql -e MYSQL_ROOT_PASSWORD=devpass -e MYSQL_DATABASE=safewalk -p 3306:3306 mysql:8.4
export DB_PASSWORD=devpass          # see .env.example for every variable
./mvnw spring-boot:run              # http://localhost:8080
```

Flyway applies `src/main/resources/db/migration` on startup; Hibernate then only **validates** the schema (`ddl-auto=validate`).

## Profiles

| Profile | How | Behaviour |
|---|---|---|
| `dev` (default) | nothing to set | local MySQL + insecure dev JWT secret fallbacks (each overridable by env var) |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | no fallbacks: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥32 bytes), `CORS_ALLOWED_ORIGINS` are required or startup fails |
| tests | automatic (`src/test/resources`) | isolated in-memory H2, Flyway off, test-only JWT secret |

No real credentials are stored in the repository; copy `.env.example` to `.env` (git-ignored) for local values.

## Tests

```bash
./mvnw test
```

- API/integration tests run the full app against in-memory H2 (schema from the JPA model).
- `MySqlMigrationTest` runs the real Flyway migrations on MySQL 8.4 via Testcontainers and lets Hibernate validate every entity.
  It needs Docker; without Docker it is reported as skipped.
- External map services are never called by tests (stub HTTP server / unreachable URLs).

## Data and limitations

- Bundled datasets (`src/main/resources/datasets/`) are imported on startup (idempotent, never deletes rows; set
  `DATASETS_IMPORT_ON_STARTUP=false` to skip): Hyderabad hospitals, police stations, women help centres, and annual state-level
  crimes-against-women statistics. `GET /api/safety-data/sources` lists what is and isn't imported and why.
- Police stations and help centres have no coordinates in the source, so they don't appear in `/nearby` results.
- Crime figures are historical, state-level and annual; there is no live incident data and no safety score.
- Geocoding/routing proxy public OpenStreetMap Nominatim and OSRM demo servers: live but not safety-aware, rate-limited, and
  subject to their usage policies (set `MAPS_USER_AGENT` with contact info).
- SOS only records the event; it does not notify anyone (`delivered: false`).
