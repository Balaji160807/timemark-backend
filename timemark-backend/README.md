# TimeMark — Backend (All Phases)

SME HR & Attendance API. Spring Boot 3 + Spring Security (JWT) + PostgreSQL + JPA/Hibernate.

Implements every layer from the original architecture diagram except the parts that
need infrastructure only you can provision (a real AWS account) or a live third-party
API key (OpenAI) to actually exercise.

## Run with Docker

```bash
docker build -t timemark-backend .
docker run -p 8080:8080 \
  -e DB_USERNAME=timemark -e DB_PASSWORD=timemark \
  -e JWT_SECRET=change-me \
  timemark-backend
```
Or use the combined `docker-compose.yml` at the top of `timemark-project/` to run
this alongside Postgres and the frontend with one command. See `../DEPLOYMENT.md`
for taking this to AWS (EC2 + RDS + CloudWatch).

## Tests

```bash
mvn test
```
Covers: geofencing distance math (`GeoUtilsTest`), payroll calculation
(`PayrollServiceTest`), registration/login (`AuthServiceTest`), leave balance
validation and approve/reject flow (`LeaveServiceTest`), and QR token signing/
validation (`QrCodeServiceTest`). All use Mockito — no live database needed.

## CI/CD
`.github/workflows/ci.yml` runs `mvn clean verify` on every push/PR, then builds and
pushes a Docker image to GitHub Container Registry on pushes to `main`. No AWS
credentials or secrets required for this part — GHCR works with just your GitHub
account. See `DEPLOYMENT.md` in the project root for taking the image further, to EC2/RDS.

## API docs
Swagger UI is live at `http://localhost:8080/swagger-ui.html` once the app is running.
Click "Authorize" and paste a JWT (no `Bearer` prefix) to try authenticated endpoints
directly from the browser.

## Prerequisites
- Java 17+
- Maven 3.9+
- Docker (for local Postgres) — or a Postgres 16 instance you already have running

## Run it locally

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run the app (downloads dependencies from Maven Central on first run)
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Environment variables (all optional, sane defaults for local dev)

| Variable | Purpose | Default |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | Postgres credentials | `timemark` / `timemark` |
| `JWT_SECRET` | HMAC signing key — **must** be overridden before any real deployment | dev placeholder |
| `JWT_EXPIRATION_MS` | Token lifetime | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Frontend origin(s), comma-separated | `http://localhost:3000` |
| `OFFICE_LAT` / `OFFICE_LNG` | Your office's coordinates — check-ins outside `OFFICE_RADIUS_METERS` of this point are rejected | Colombo, Sri Lanka (placeholder) |
| `OFFICE_RADIUS_METERS` | Geofence radius | `200` |
| `PAYROLL_WORKING_DAYS` | Working days/month used for per-day rate calc | `26` |
| `QR_SECRET` | HMAC key signing the daily office check-in QR code | dev placeholder |
| `NOTIFICATIONS_ENABLED` | Turn on email notifications (leave decisions, check-in reminders) | `false` |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | Email delivery — leave blank to no-op (logs instead of sending) | blank |
| `REMINDER_CRON` | Cron schedule for the "haven't checked in" reminder job | `0 30 9 * * MON-FRI` |
| `OPENAI_API_KEY` | Enables `/api/insights/team` — blank means the endpoint returns a "not configured" message instead of erroring | blank |
| `OPENAI_MODEL` | Model used for AI insights | `gpt-4o-mini` |
| `S3_ENABLED` / `S3_BUCKET` / `AWS_REGION` | Optional payslip upload to S3 — disabled by default, uses AWS SDK's default credential chain | disabled |

## API reference (quick curl walkthrough)

**Register an HR user, then an employee (email is optional but needed for notifications):**
```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{
  "username":"hr_nimali","password":"pass123","fullName":"Nimali Silva","email":"nimali@example.com",
  "role":"HR","department":"HR","designation":"HR Officer","salary":70000
}'

curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{
  "username":"priya","password":"pass123","fullName":"Priya Fernando","email":"priya@example.com",
  "role":"EMPLOYEE","department":"Engineering","designation":"Software Engineer","salary":85000
}'
```

**Login (get a JWT):**
```bash
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{
  "username":"priya","password":"pass123"
}'
# => { "token": "...", "username": "priya", "fullName": "Priya Fernando", "role": "EMPLOYEE" }
```

**Check in via GPS (as priya, using her token):**
```bash
TOKEN="paste-the-token-here"
curl -X POST localhost:8080/api/attendance/checkin -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"latitude":6.9271,"longitude":79.8612}'
```
Coordinates must be within `OFFICE_RADIUS_METERS` of `OFFICE_LAT`/`OFFICE_LNG` (defaults
above are Colombo, so the example call succeeds against the default config).

**Check in via QR code (alternative to GPS):**
```bash
curl localhost:8080/api/attendance/qr-code -H "Authorization: Bearer $HR_TOKEN"
# => { "token": "OFFICE_CHECKIN:2026-07-31:abcd1234...", "imageBase64": "data:image/png;base64,..." }

curl -X POST localhost:8080/api/attendance/checkin-qr -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"token":"OFFICE_CHECKIN:2026-07-31:abcd1234..."}'
```

**Request leave:**
```bash
curl -X POST localhost:8080/api/leave -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{
  "type":"CASUAL","fromDate":"2026-08-10","toDate":"2026-08-11","reason":"Family event"
}'
```

**As HR — view pending leave and approve (this also emails the employee if notifications are on):**
```bash
HR_TOKEN="paste-hr-token"
curl localhost:8080/api/leave/pending -H "Authorization: Bearer $HR_TOKEN"
curl -X POST localhost:8080/api/leave/1/approve -H "Authorization: Bearer $HR_TOKEN"
```

**Payroll — your own payslip (JSON or PDF), or the whole team as HR:**
```bash
curl localhost:8080/api/payroll/me -H "Authorization: Bearer $TOKEN"
curl "localhost:8080/api/payroll/me?month=2026-06" -H "Authorization: Bearer $TOKEN"
curl localhost:8080/api/payroll/me/payslip -H "Authorization: Bearer $TOKEN" -o payslip.pdf

curl localhost:8080/api/payroll/team -H "Authorization: Bearer $HR_TOKEN"
```

**AI insights (needs `OPENAI_API_KEY` set — otherwise returns a friendly "not configured" message):**
```bash
curl localhost:8080/api/insights/team -H "Authorization: Bearer $HR_TOKEN"
```

## Endpoint summary

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create an employee account |
| POST | `/api/auth/login` | public | Get a JWT |
| POST | `/api/attendance/checkin` | authenticated | Check in via GPS |
| POST | `/api/attendance/checkin-qr` | authenticated | Check in via office QR code |
| POST | `/api/attendance/checkout` | authenticated | Check out for today |
| GET | `/api/attendance/me` | authenticated | Your attendance history |
| GET | `/api/attendance/team` | HR/MANAGER/ADMIN | Today's attendance, all employees |
| GET | `/api/attendance/qr-code` | HR/MANAGER/ADMIN | Today's rotating office QR code |
| POST | `/api/leave` | authenticated | Submit a leave request |
| GET | `/api/leave/me` | authenticated | Your leave requests |
| GET | `/api/leave/pending` | HR/MANAGER/ADMIN | All pending requests |
| POST | `/api/leave/{id}/approve` | HR/MANAGER/ADMIN | Approve + deduct balance + email |
| POST | `/api/leave/{id}/reject` | HR/MANAGER/ADMIN | Reject + email |
| GET | `/api/payroll/me?month=YYYY-MM` | authenticated | Your payslip as JSON |
| GET | `/api/payroll/me/payslip?month=YYYY-MM` | authenticated | Your payslip as a downloadable PDF |
| GET | `/api/payroll/team?month=YYYY-MM` | HR/MANAGER/ADMIN | Payroll for every employee |
| GET | `/api/insights/team` | HR/MANAGER/ADMIN | AI-generated summary of today's attendance/leave |

## Design decisions worth being able to explain in an interview
- **Employee doubles as the security principal** (implements `UserDetails` directly) instead of
  a separate `User` table. This is a deliberate MVP simplification — fine for a single-tenant
  app, and the natural next step (documented, not yet built) is splitting identity out once
  the product needs multi-company support or SSO.
- **Stateless JWT auth**: no server-side session store, so this backend horizontally scales
  behind a load balancer without sticky sessions — matches the AWS EC2 layer in the architecture.
- **Geofencing uses plain Haversine distance**, not the Google Maps Distance Matrix API — cheaper
  and accurate enough for "is this point within N meters of the office."
- **The QR code needs no database table.** It's an HMAC-signed token containing the date, verified
  by recomputing the signature — so it's stateless, can't be forged without the server's secret,
  and automatically expires at midnight without any cleanup job.
- **Payroll is computed on read, not stored.** No `payroll` table — every request recomputes
  attendance + leave for the month, always consistent with the source data.
- **Notifications degrade gracefully.** If `NOTIFICATIONS_ENABLED=false` or SMTP isn't configured,
  `EmailService` logs what it would have sent instead of failing — so leave approval, which
  triggers a notification, never breaks just because email isn't set up in a given environment.
- **S3Service only exists as a Spring bean when `app.s3.enabled=true`** (via
  `@ConditionalOnProperty`), so the app boots cleanly with zero AWS credentials in local dev, CI,
  or any demo that hasn't configured it.
- **AI insights hasn't been tested against a live OpenAI key** in the environment this was built
  in (no outbound network access there) — it's written to OpenAI's documented REST contract, but
  verify it end-to-end with your own key before demoing it live.

## What's left
- **AWS deployment** — documented step-by-step in `../DEPLOYMENT.md`, needs your own AWS account.
- **Automated CI→EC2 deploy** — template described in `DEPLOYMENT.md`, needs `EC2_HOST`/`EC2_SSH_KEY` secrets.
- **Multi-company/multi-tenant support** — current design is single-company; noted above as a
  deliberate MVP simplification, real limitation if this needs to serve many companies as
  separate customers rather than one deployment per company.
- **Push notifications** — only email is implemented, not push.
- **Employee registration UI** — employees are currently created via the `/api/auth/register`
  API directly (see curl examples above); there's no HR-facing "add employee" screen in the
  frontend yet.
