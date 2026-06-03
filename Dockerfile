# =============================================================================
# 浪海博客 - Dockerfile (Trae/Docker Rollout)
# =============================================================================
#
# 【基础构建】vs 【完整运行】说明：
#
# 基础构建（docker build 即可完成）：
#   - 仅需 JDK 8 + Maven 环境
#   - 执行 mvn package -DskipTests 生成 jar 包
#   - 不依赖任何外部服务，构建一定成功
#   - 容器启动后 SSH 服务可用，方便 Trae 进入容器调试
#
# 完整运行（需要外部服务 + 配置）：
#   - 必需：MySQL（3306）、Redis（6379）、MinIO（9000）
#   - 可选：Elasticsearch（9200）、RabbitMQ（5672）
#   - 需要在 application.yml 中配置各服务的连接地址、用户名、密码
#   - 需要导入 sql/langhaiblogs.sql 并初始化角色数据
#   - 完整运行可通过环境变量或挂载配置文件覆盖 application.yml
# =============================================================================

FROM maven:3.8-openjdk-8

ARG SSH_ROOT_PASSWORD=trae

RUN apt-get update && \
    apt-get install -y --no-install-recommends openssh-server && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /run/sshd && \
    ssh-keygen -A && \
    echo "root:${SSH_ROOT_PASSWORD}" | chpasswd && \
    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config && \
    sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn package -DskipTests -B

EXPOSE 22 2086 20202

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
