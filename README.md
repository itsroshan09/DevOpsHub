# DevOpsHub

DevOpsHub is an engineering incident management platform designed to centralize incident reporting, assignment, investigation updates, and resolution history.

The project is being developed incrementally as a modular monolith, with a Java backend and PostgreSQL database.

## Current Status

The backend includes authentication, role-based authorization, a software service registry, incident management, investigation updates, audit-event support, and dashboard summary statistics. The latest verified local test run passed 106 tests with zero failures or errors. Run the full verification command again to confirm the current state after any changes.
## Technology Stack

- Java 21
- Spring Boot 4.0.8
- Maven
- Spring MVC
- Spring Data JPA and Hibernate
- PostgreSQL
- Flyway database migrations
- Spring Security
- JSON Web Tokens (JWT)
- Jakarta Bean Validation
- JUnit and Spring Boot testing support

## Architecture

The application follows a feature-oriented package structure:

- `auth` — registration and login
- `user` — user model and response representation
- `security` — JWT processing, password configuration, and authorization rules
- `service` — software service registry
- `incident` — incident API and business logic
- `audit` — audit-event persistence
- `common` — shared exception handling

The application entry point is `DevopshubApplication`.

## Prerequisites

Install:

- JDK 21
- Git
- Docker Desktop with Docker Compose support

Maven is provided through the Maven Wrapper, so a separate Maven installation is not required.

## Configuration

The application reads the following environment variables:

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret used for JWT signing |
When running the application directly on your host machine, use `jdbc:postgresql://localhost:5433/devopshub` for `DB_URL` while PostgreSQL is running through the supplied Compose configuration. The Compose application service uses `jdbc:postgresql://postgres:5432/devopshub` internally; do not override it with the host-side URL when starting the stack through Compose.

## Start the Application with Docker Compose

In PowerShell, from the project root, configure the required environment variables in your current session. Use your own local database password and a strong, randomly generated Base64 JWT secret.

```powershell
$env:DB_PASSWORD = "your-local-database-password"

$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
} finally {
    $rng.Dispose()
}

docker compose up -d --build
docker compose ps
docker compose logs --tail 100 app
```

Compose builds the application image and starts the backend and PostgreSQL. The backend waits for PostgreSQL's health check before starting.

The API is published on `127.0.0.1:8080`, and PostgreSQL is published on `127.0.0.1:5433`. Flyway applies pending database migrations at application startup.

To stop the containers, run:

```powershell
docker compose down
```

This preserves the database volume. To remove the database volume as well, use `docker compose down -v` only when you intentionally want to delete the local database data.

## Run the Backend

Configure the database and JWT environment variables in the same PowerShell session:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/devopshub"
$env:DB_USERNAME = "devopshub_app"
$env:DB_PASSWORD = "your-local-database-password"
$env:JWT_SECRET = "your-local-jwt-signing-secret"
```

Use a cryptographically strong JWT signing secret compatible with the application's JWT configuration.

Then start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway is enabled and applies pending migrations at application startup. Hibernate is configured to validate the schema rather than create or update it automatically.

## Run Tests

Run the full build and test verification from PowerShell:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

The command compiles the project, runs the automated tests, and verifies the Maven build. GitHub Actions runs the equivalent Maven Wrapper command with Java 21 and PostgreSQL 18.

## API Controllers

The current REST controllers are:

- `AuthController`
- `ServiceController`
- `IncidentController`

The available routes and permissions are defined by the implementation. Consult the controller classes and `SecurityConfig` for the current API behavior.

Authentication endpoints include registration and login. Protected endpoints use JWT bearer authentication.

## Database Migrations

Flyway migrations are stored in `src/main/resources/db/migration`:

- `V1__initial_schema.sql`
- `V2__create_incident_updates.sql`
- `V3__create_audit_events.sql`
- `V4__create_users.sql`
- `V5__add_incident_assignee.sql`
- `V6__add_user_password_hash.sql`
- `V7__create_services.sql`
- `V8__add_audit_event_actor.sql`

Do not edit an already-applied migration to change an existing database. Add a new versioned migration for future schema changes.

## Planned Work

The broader project plan includes completing and verifying incident workflows, authorization rules, investigation history, dashboard statistics, filtering and pagination, API documentation, continuous integration, containerized execution, and a React/TypeScript frontend.

These are roadmap items, not claims that every capability is currently implemented.

## Security Notes

- Passwords must not be stored in plaintext.
- Do not commit secrets or real credentials.
- Protected API requests require valid authentication and appropriate authorization.
- Keep important business changes and corresponding audit records transactionally consistent.

## License

No project license has been specified yet.