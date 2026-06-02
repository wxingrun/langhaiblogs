FROM maven:3.8-openjdk-8 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/target/langhaiblogs~v0.0.3.jar /app/langhaiblogs~v0.0.3.jar
EXPOSE 2086
ENTRYPOINT ["java", "-jar", "/app/langhaiblogs~v0.0.3.jar"]