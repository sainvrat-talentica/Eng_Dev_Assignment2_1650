FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY platform-lib platform-lib
COPY analytics-lib analytics-lib
COPY analytics-service analytics-service
RUN mvn -q -pl analytics-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/analytics-service/target/analytics-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
