FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY comun/ /comun/
COPY ms-usuarios/ ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
