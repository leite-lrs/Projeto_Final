FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /work
COPY --from=build /workspace/target/quarkus-app /work/quarkus-app
EXPOSE 3002
ENTRYPOINT ["java", "-jar", "/work/quarkus-app/quarkus-run.jar"]
