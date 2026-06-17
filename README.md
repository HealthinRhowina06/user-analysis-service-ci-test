# analysis

Standalone Spring Boot microservice for user topic analytics.

## Endpoints

- `GET /rs/analysis/users/{userId}/summary`

Detailed current analytics features and formulas:

- `docs/ANALYTICS_FEATURES_AND_FORMULAS.md`

Response now includes module-wise topic details:

- module-level topic attendance (`attendedTopics`, `totalTopics`, `attendancePercentage`)
- topic-level score and completion
- topic-level question progress (`questionsCount`, `attendedQuestionsCount`, `topicCompletionPercentage`)

Optional request headers:

- `X-Institution-Id`

## Key Design Notes

- Read-only transactional analysis services.
- Batched JDBC queries to avoid nested DB calls in loops.
- Focused endpoints (no catch-all endpoint).
- Direct controller-to-service flow (no Spring Integration channels/gateway layer).

## Build and Run

```bash
mvn clean test
mvn spring-boot:run
```

Java 25 is required.

Docker build options:

```bash
mvn -Pdocker -DskipTests package
docker compose up --build
docker compose -f docker_env/dev.yml up -d
docker compose -f docker_env/prod.yml up -d
```

## Config

Main properties live in `src/main/resources/application.properties`:

- datasource
- cloud config
- Eureka client
- `analysis.query.xml.location` (XML query file path; can come from Cloud Config)
- default pass percentage and excluded qtypes

Important runtime environment variables:

- `CLOUDCONFIG_HOST`, `CLOUDCONFIG_PORT`, `CLOUDCONFIG_USER`, `CLOUDCONFIG_PASSWORD`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `EUREKA_HOST`, `EUREKA_PORT`, or `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `SERVER_PORT`
