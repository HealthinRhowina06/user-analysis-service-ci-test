FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/user-analysis-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","/app/app.jar"]
