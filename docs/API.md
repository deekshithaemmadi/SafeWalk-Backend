# SafeWalk Backend API

Base URL (local): `http://localhost:8080`. All bodies are JSON (`Content-Type: application/json`).
Protected endpoints need `Authorization: Bearer <token>` (token from register/login).

## Conventions

**Error body** (every 4xx/5xx):

```json
{
  "timestamp": "2026-10-11T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: Phone number must be exactly 10 digits.",
  "path": "/api/auth/register",
  "fieldErrors": { "phone": "Phone number must be exactly 10 digits." }
}
```

`fieldErrors` is present only for body validation failures. The frontend should display `message`.

| Status | When |
|---|---|
| 400 | validation failure, malformed JSON, missing/invalid query parameter, business rule (e.g. min. 2 contacts) |
| 401 | missing/invalid/expired token, wrong login credentials |
| 403 | authenticated but not allowed (other user's resource, non-moderator moderating) |
| 404 | resource/endpoint not found, no route between points |
| 405 / 415 | wrong HTTP method / Content-Type not JSON |
| 409 | duplicate email/phone/contact phone |
| 502 | upstream map service returned an error |
| 503 | database or upstream map service unreachable |
| 504 | upstream map service timed out |
| 500 | unexpected error (generic message, details only in server logs) |

Paginated endpoints accept `page` (0-based), `size` (max 100) and `sort` (e.g. `createdAt,desc`) and return a Spring `Page`
(`content`, `totalElements`, `totalPages`, `number`, `size`, ...).

## Auth (public)

### `POST /api/auth/register` → 201
```json
{
  "name": "Asha Rao", "email": "asha@example.com", "phone": "9876543210", "password": "secret123",
  "emergencyContacts": [
    { "name": "Mom", "phone": "9876500001", "relation": "Mother" },
    { "name": "Dad", "phone": "9876500002", "relation": "Father" }
  ]
}
```
Rules: name ≤150, valid email ≤190 (stored lowercase), phone 10 digits, password 6–72 chars.
`emergencyContacts` optional; completely blank rows are ignored, partially filled rows → 400; contact phones distinct, 10 digits.
Response:
```json
{ "token": "eyJhbGciOi...", "user": { "id": 1, "name": "Asha Rao", "email": "asha@example.com", "phone": "9876543210",
  "emergencyContacts": [ { "id": 1, "name": "Mom", "phone": "9876500001", "relation": "Mother" } ] } }
```
409 on duplicate email/phone.

### `POST /api/auth/login` → 200
`{ "email": "asha@example.com", "password": "secret123" }` → same body as register. Wrong email or password → 401 `"Invalid email or password."`

## Profile (auth)

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/api/users/me` | – | `UserResponse` (as above) |
| PUT | `/api/users/me` | `{ "name", "email", "phone" }` (all required) | `UserResponse`; 409 if email/phone used by another account |
| PUT | `/api/users/me/password` **(new)** | `{ "currentPassword": "secret123", "newPassword": "newSecret1" }` | 204; 400 if current password wrong |

## Emergency contacts (auth)

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/api/emergency-contacts` | – | `[{ "id", "name", "phone", "relation" }]` |
| POST | `/api/emergency-contacts` | `{ "name", "phone", "relation" }` | 201 contact; 409 duplicate phone |
| PUT | `/api/emergency-contacts/{id}` | `{ "name", "phone", "relation" }` | 200 contact; 403 not yours; 404 |
| DELETE | `/api/emergency-contacts/{id}` | – | 204; 400 `"You must keep at least 2 emergency contacts."` |

## Geocoding & routing (public, live third-party data)

These proxy OpenStreetMap **Nominatim** and the public **OSRM** demo server. Successful responses are the upstream JSON,
unchanged. Routes are generic (OSRM `driving` profile on the demo server) and **not safety-aware**.

### `GET /api/geocode?q=Charminar`
`q` required, 2–200 chars; results restricted to India / Telangana bounding box. 200 → Nominatim array:
```json
[ { "display_name": "Charminar, Hyderabad, Telangana, India", "lat": "17.3616", "lon": "78.4747", "address": { } } ]
```
Errors: 400 (missing/short q), 502 / 503 / 504 (upstream error / unreachable / timeout).

### `GET /api/route?startLat=17.385&startLon=78.4867&endLat=17.4401&endLon=78.3489`
All four required; lat −90..90, lon −180..180, start ≠ end. 200 → OSRM response (`code: "Ok"`, `routes[]` with GeoJSON `geometry`,
`distance` m, `duration` s, alternatives). 404 when OSRM finds no route. Upstream failures return 502/503/504 with the error body
**plus** legacy fields so old clients don't crash:
```json
{ "status": 504, "error": "Gateway Timeout", "message": "Routing service timed out. Please try again.",
  "path": "/api/route", "code": "BackendRoutingError", "routes": [] }
```

## Help locations (public, static dataset)

Records are imported on startup from the bundled CSVs in `src/main/resources/datasets/` (idempotent upsert keyed on a
deterministic `sourceKey`; rows are never deleted; disable with `DATASETS_IMPORT_ON_STARTUP=false`). Every record carries
`source`/`sourceYear`. Nothing is fabricated: missing coordinates/phones stay `null`. **No live data, no safety scores.**

| Type | File | Records | Notes |
|---|---|---|---|
| `HOSPITAL` | `hyderabad_hospitals.csv` | 1,283 | Hyderabad Hospitals 2018 + Health Facilities 2025, with coordinates. Same-name entries within 50 m in one source are de-duplicated. |
| `POLICE` | `hyderabad_police_stations.csv` | 79 | No coordinates in source (not in `/nearby`); some phones missing. |
| `WOMEN_HELP` | `women_help_centres_selected.csv` | 169 | NGO directory (support type, sectors, tier); no coordinates or phones. `district` = Hyderabad only if the address mentions Hyderabad/Secunderabad. |

`HelpLocationResponse`:
```json
{ "id": 1, "type": "POLICE", "name": "...", "category": "...", "district": "Hyderabad", "locality": "...", "address": "...",
  "latitude": 17.44, "longitude": 78.45, "phone": "...", "supportSectors": null, "tier": null,
  "sourceYear": "2024", "source": "...", "hasCoordinates": true }
```

| Method | Path | Params | Response |
|---|---|---|---|
| GET | `/api/help-locations` | `type` (HOSPITAL/POLICE/WOMEN_HELP), `q` (name/locality/address), `district` (case-insensitive), paging | Page of `HelpLocationResponse` (all filters now combine) |
| GET | `/api/help-locations/{id}` **(new)** | – | `HelpLocationResponse`; 404 |
| GET | `/api/help-locations/nearby` **(new)** | `lat`, `lon` required; `radiusKm` (default 5, max 50), `type`, `limit` (default 20, max 100) | `[{ "location": HelpLocationResponse, "distanceKm": 0.42 }]` nearest first; straight-line distance; records without coordinates excluded |
| GET | `/api/help-locations/meta` **(new)** | – | see below |

```json
{ "totalRecords": 0, "countsByType": { "HOSPITAL": 0, "POLICE": 0, "WOMEN_HELP": 0 }, "recordsWithCoordinates": 0,
  "sources": [], "dataNature": "STATIC_DATASET", "live": false,
  "limitations": [ "Records come from static, imported datasets; they are not live and are not verified in real time.", "..." ] }
```

## Safety data (public, static dataset) **(new)**

### `GET /api/safety-data/women-crime-stats?state=Telangana&fromYear=2014&toYear=2021`
All params optional (`state` case-insensitive; years 1900–2100, `fromYear` ≤ `toYear`, otherwise 400). Annual **state-level**
counts from `women_crime_categories.csv` (Andhra Pradesh 2011–2013, Telangana 2011–2021):
```json
{
  "records": [
    { "state": "Telangana", "regionContext": "Telangana", "year": 2014, "reportedSeparately": true, "possiblyIncomplete": false,
      "counts": { "RAPE": 979, "KIDNAPPING_AND_ABDUCTION": 711, "DOWRY_DEATHS": 563, "CRUELTY_BY_HUSBAND_OR_RELATIVES": 6369,
                  "HUMAN_TRAFFICKING": 0, "ASSAULT_ON_MODESTY": 3188, "SEXUAL_HARASSMENT": 620, "IMMORAL_TRAFFIC": 192 },
      "total": 14136 }
  ],
  "categoryLabels": { "RAPE": "Rape", "...": "..." },
  "granularity": "STATE_ANNUAL", "dataNature": "STATIC_DATASET", "live": false,
  "source": "State-level crimes against women, annual (women_crime_categories.csv)",
  "limitations": [ "...must not be used as a safety score for any route, area or place.", "..." ]
}
```
- Telangana 2011–2013 rows are all zeros in the source (Telangana was part of undivided AP): returned as `reportedSeparately: false`
  with `null` counts, not as zero crime.
- `possiblyIncomplete: true` when a year's total is below 25% of that state's median total (Telangana 2020 = 874 and
  2021 = 807 vs ~15k in other years). Show a warning for these years.
- Category counts don't necessarily add up to `total` (as in the source).

### `GET /api/safety-data/sources`
Lists every supplied dataset, whether it is imported, record count, which endpoint serves it, and notes. Not imported (with
reasons in `notes`): `SafeWalk_Andhra_Pradesh_2002_2013.csv` (unlabelled measure, values appear shifted between columns) and the
three PDFs (unstructured reference documents).

## Trips (auth)

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/trips` | `{ "originLabel", "origin": {"latitude","longitude"}, "destinationLabel", "destination": {...} }` | 201 `TripResponse` (`PLANNED`) |
| GET | `/api/trips` | paging | Page of own trips, newest first |
| GET | `/api/trips/{id}` | – | `TripResponse` |
| POST | `/api/trips/{id}/start` / `end` / `cancel` | – | `TripResponse` (`ACTIVE` / `COMPLETED` / `CANCELLED`); invalid transition → 400 |
| POST | `/api/trips/{id}/location` | `{ "latitude", "longitude" }` | `TripResponse`; only while `ACTIVE` |

`TripResponse`: `id, status, originLabel, originLatitude, originLongitude, destinationLabel, destinationLatitude,
destinationLongitude, lastLatitude, lastLongitude, startedAt, endedAt, createdAt, updatedAt`.

## SOS (auth)

`POST /api/sos` → 201
```json
{ "message": "Need help", "shareLocation": true, "latitude": 17.43, "longitude": 78.44, "accuracy": 15, "requestedAt": "2026-10-11T20:00:00Z" }
```
```json
{ "id": 5, "delivered": false, "message": "SOS recorded. SafeWalk does not automatically contact anyone ...", "createdAt": "..." }
```
SafeWalk **only records** the SOS; it does not call, SMS or notify contacts/police (`delivered` is always `false`). Location is
stored only when `shareLocation` is `true`. `GET /api/sos` lists the user's own SOS events.

## Community reports (auth)

| Method | Path | Body / params | Response |
|---|---|---|---|
| POST | `/api/community-reports` | `{ "category", "description" (10–1000), "location"?, "latitude"?, "longitude"? }` | 201, status `PENDING` |
| GET | `/api/community-reports` | `status`, `category`, paging | `VERIFIED` reports + caller's own reports; `status` filter now applied |
| GET | `/api/community-reports/{id}` | – | report (owner, or anyone if VERIFIED) |
| PUT | `/api/community-reports/{id}` | partial update (owner only) | report |
| POST | `/api/community-reports/{id}/moderate` | `{ "status": "VERIFIED" | "REJECTED", "note"? }` | MODERATOR role only |

Report coordinates are validated (lat −90..90, lon −180..180). Reports are user-submitted and unverified until moderated.

## Chatbot (auth)

`POST /api/chatbot/message` `{ "message": "how does sos work" }` →
`{ "answer": "...", "followUps": ["..."], "matchedId": "sos", "predefined": true }`. Deterministic FAQ matching, not an AI model.

---

## Frontend changes required

No existing paths, methods or success payloads were changed. Behavioural changes to handle:

1. **`GET /api/geocode` errors**: previously returned `200 []` on any failure; now returns 400/502/503/504 with the error body.
   Check `response.ok` and show `message` (e.g. "Geocoding service is currently unavailable...") instead of mapping over the body.
2. **`GET /api/route` errors**: previously `200 {"code":"BackendRoutingError","routes":[]}`; now the same `code`/`routes` fields
   arrive with status 400/404/502/503/504 plus `message`. Code that only checks `routes.length` keeps working if it parses
   non-2xx bodies; ideally check `response.ok` and show `message`. Invalid coordinates or identical start/end now return 400.
3. **Registration**: a partially filled emergency-contact row now returns 400 (previously could cause a 500); completely blank rows
   are still ignored. Passwords longer than 72 characters return 400.
4. **Optional new features**: `PUT /api/users/me/password`, `GET /api/help-locations/{id}`, `/nearby`, `/meta` (show the
   `limitations` text near help-location lists), `GET /api/safety-data/women-crime-stats` and `/sources`. If the frontend shows
   crime figures, label them as historical state-level statistics, never as a route/area safety score.
5. **Community report list**: the `status` query parameter is now honoured (it was silently ignored before).
