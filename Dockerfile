# ============================================================
# 阶段 1: 构建阶段 — 使用 Maven + JDK 8 编译项目
# ============================================================
FROM maven:3.8-openjdk-8-slim AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:resolve -B -q

COPY src ./src

RUN mvn package -DskipTests -B -q

# ============================================================
# 阶段 2: 运行阶段 — JDK 8 + OpenSSH Server
# ============================================================
FROM openjdk:8-jdk-slim

WORKDIR /app

# -----------------------------------------------------------
# 安装并配置 OpenSSH Server（非交互式，可重复构建）
# -----------------------------------------------------------
# 注意: 以下 root 密码仅用于开发/调试环境容器，不可用于生产。
#       生产环境请使用 SSH Key 认证或更强的密码策略。
RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-server \
    && mkdir -p /var/run/sshd \
    && echo 'root:root' | chpasswd \
    && sed -i 's/^#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config \
    && sed -i 's/^PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config \
    && sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config \
    && sed -i 's/^PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# -----------------------------------------------------------
# 从构建阶段复制产物 + 源代码（方便调试）
# -----------------------------------------------------------
COPY --from=build /app/target/*.jar /app/app.jar

COPY --from=build /app/src /app/src
COPY --from=build /app/pom.xml /app/pom.xml

# -----------------------------------------------------------
# 暴露端口
# 22   — SSH（容器运行时建议通过 -p 2222:22 映射）
# 2086 — Spring Boot 应用端口
# -----------------------------------------------------------
EXPOSE 22 2086

# -----------------------------------------------------------
# 启动脚本:
# 1. 生成 SSH 主机密钥（首次运行时）
# 2. 启动 sshd 守护进程
# 3. tail -f /dev/null 保持容器存活
#
# 说明: 容器默认不会启动 Spring Boot 应用，
#       你可以通过 SSH 进入容器后手动运行：
#         java -jar /app/app.jar
#       （前提是先确保外部 MySQL/Redis/MinIO 等服务已就绪）
# -----------------------------------------------------------
CMD ["sh", "-c", "ssh-keygen -A && /usr/sbin/sshd && tail -f /dev/null"]