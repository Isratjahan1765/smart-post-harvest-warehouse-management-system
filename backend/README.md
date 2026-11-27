# sphwms-backend

Backend API for SPHWMS (warehouse management system).

## Stack
- Java 17, Spring Boot 3.x, Maven
- MySQL 8
- Spring Security + JWT

## Run locally
1. Install JDK 17, MySQL 8, Maven (or use the IntelliJ built-in Maven).
2. Copy `src/main/resources/application-example.properties` to
   `src/main/resources/application.properties` and set your MySQL password and JWT secret.
3. Start MySQL. The database `sphwms` is created automatically.
4. Run `SphwmsApplication` from IntelliJ, or `mvn spring-boot:run`.
5. API runs at http://localhost:8080

## Test users (development only)
| Username | Password | Role |
|---|---|---|
| manager | <your-password> | MANAGER |
| admin | <your-password> | ADMIN |
| driver1 | <your-password> | DRIVER |

## Postman
Import `docs/api-collection.json` into Postman. Set the environment variable
`baseUrl = http://localhost:8080`. Log in first; the token is saved automatically.

## Branches and commits
- `main` stable, `develop` in progress, `feature/<name>` per feature
- Commit style: `feat:`, `fix:`, `docs:`

## Docs
See `docs/features.md` for the step-by-step flow of every feature.
