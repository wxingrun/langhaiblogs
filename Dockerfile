FROM maven:3.8.4-jdk-8

# 设置工作目录为 /app
WORKDIR /app

# 安装 OpenSSH Server
RUN apt-get update && \
    apt-get install -y openssh-server && \
    rm -rf /var/lib/apt/lists/*

# 配置 SSH：允许 root 登录，设置 root 密码
RUN mkdir -p /var/run/sshd && \
    echo 'root:trae123' | chpasswd && \
    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config

# 暴露端口：SSH 22，应用 2086
EXPOSE 22 2086

# 将项目文件复制到容器中（先复制 pom.xml 单独构建依赖层，加快构建速度）
COPY pom.xml .
COPY src ./src

# 预先下载 Maven 依赖
RUN mvn dependency:go-offline -B

# 创建启动脚本：启动 SSH 服务并保持容器运行
RUN echo '#!/bin/bash' > /app/start.sh && \
    echo '/usr/sbin/sshd -D &' >> /app/start.sh && \
    echo 'tail -f /dev/null' >> /app/start.sh && \
    chmod +x /app/start.sh

# 设置容器启动命令
CMD ["/app/start.sh"]
