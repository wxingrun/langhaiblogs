# 浪海博客 Docker 部署指南

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+

## 快速启动

```bash
# 在项目根目录执行
docker-compose up -d
```

启动后访问 http://localhost:2086 即可看到博客首页。

## 服务说明

| 服务 | 端口 | 访问地址 | 账号/密码 |
|------|------|----------|-----------|
| 应用服务 | 2086 | http://localhost:2086 | 需注册 |
| MySQL | 3306 | localhost:3306 | root / langhai123 |
| Redis | 6379 | localhost:6379 | 无密码 |
| MinIO API | 9000 | http://localhost:9000 | minioadmin / minioadmin |
| MinIO Console | 9001 | http://localhost:9001 | minioadmin / minioadmin |
| RabbitMQ | 5672 | localhost:5672 | langhai / langhai123 |
| RabbitMQ 管理 | 15672 | http://localhost:15672 | langhai / langhai123 |

## 数据持久化

- MySQL 数据卷：`mysql-data`
- Redis 数据卷：`redis-data`
- MinIO 数据卷：`minio-data`

数据存储在 Docker 管理的卷中，容器删除后数据不会丢失。

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 停止所有服务
docker-compose down

# 停止并删除数据卷（重置所有数据）
docker-compose down -v

# 重新构建并启动
docker-compose up -d --build
```

## 环境变量覆盖

如需修改默认配置，可通过环境变量覆盖，支持以下变量：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| MYSQL_HOST | mysql | MySQL 主机地址 |
| MYSQL_PORT | 3306 | MySQL 端口 |
| MYSQL_USER | root | MySQL 用户名 |
| MYSQL_PASSWORD | langhai123 | MySQL 密码 |
| REDIS_HOST | redis | Redis 主机地址 |
| REDIS_PORT | 6379 | Redis 端口 |
| REDIS_PASSWORD | (空) | Redis 密码 |
| MINIO_ENDPOINT | http://minio:9000 | MinIO 连接地址 |
| MINIO_ACCESS_KEY | minioadmin | MinIO 访问密钥 |
| MINIO_SECRET_KEY | minioadmin | MinIO 秘密密钥 |
| RABBITMQ_HOST | rabbitmq | RabbitMQ 主机地址 |
| RABBITMQ_PORT | 5672 | RabbitMQ 端口 |
| RABBITMQ_USER | langhai | RabbitMQ 用户名 |
| RABBITMQ_PASS | langhai123 | RabbitMQ 密码 |

在 `docker-compose.yml` 的 `app` 服务中添加 `environment` 即可覆盖：
```yaml
app:
  environment:
    - MYSQL_PASSWORD=my-secret-password
```