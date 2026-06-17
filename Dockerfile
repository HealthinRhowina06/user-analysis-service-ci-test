FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 9035
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
