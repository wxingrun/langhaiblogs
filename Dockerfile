FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=builder /build/target/langhaiblogs-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 2086
ENTRYPOINT ["java", "-jar", "app.jar"]
