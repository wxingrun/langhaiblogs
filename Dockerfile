FROM maven:3.8.6-openjdk-8 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
# 解决国内网络可能导致的依赖下载慢问题，可以通过配置 settings.xml，但这里直接打包
RUN mvn clean package -DskipTests

FROM openjdk:8-jre-slim

# 优化: 调整时区，设置一些JVM参数等
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 将jar包复制到/app目录
COPY --from=builder /build/target/*.jar /app/app.jar

EXPOSE 2086

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
