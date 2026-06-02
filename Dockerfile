FROM maven:3.8.8-openjdk-8-slim AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=builder /build/target/langhaiblogs~v0.0.3.jar /app/langhaiblogs.jar
EXPOSE 2086
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/langhaiblogs.jar"]
