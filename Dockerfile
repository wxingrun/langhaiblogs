FROM openjdk:8-jre-slim

LABEL maintainer="langhaiblogs"

WORKDIR /app

COPY target/langhaiblogs~v0.0.3.jar /app/app.jar

EXPOSE 2086

ENTRYPOINT ["java", "-jar", "app.jar"]
