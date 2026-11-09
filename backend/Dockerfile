# Etapa 1: compila el proyecto con Maven
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Etapa 2: imagen final, solo con el JRE y el .jar (más pequeña y segura)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/target/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
