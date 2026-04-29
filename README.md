# analysis

Standalone Spring Boot microservice for user topic analytics.

## Endpoints

- `GET /analysis/users/{userId}/summary`

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

## Config

Main properties live in `src/main/resources/application.properties`:

- datasource
- cloud config
- Eureka client
- `analysis.query.xml.location` (XML query file path; can come from Cloud Config)
- default pass percentage and excluded qtypes
