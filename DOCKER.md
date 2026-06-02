# 浪海博客 Docker 部署指南

## 前置条件

- Docker >= 20.10
- Docker Compose >= 2.0

## 一键启动

```bash
docker-compose up -d
```

启动后访问 http://localhost:2086 即可看到博客首页。

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| app | 2086 | 博客应用 |
| MySQL | 3306 | 数据库（库名 langhaiblogs） |
| Redis | 6379 | 缓存（database 9） |
| MinIO | 9000 / 9001 | 对象存储（9001 为控制台） |

## 默认账号

| 服务 | 用户名 | 密码 |
|------|--------|------|
| MySQL | root | root123 |
| MinIO | minioadmin | minioadmin |

## 环境变量覆盖

可通过 `.env` 文件或 `docker-compose.yml` 中的 `environment` 覆盖配置：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| SPRING_DATASOURCE_URL | jdbc:mysql://mysql:3306/langhaiblogs?... | 数据库连接地址 |
| SPRING_DATASOURCE_USERNAME | root | 数据库用户名 |
| SPRING_DATASOURCE_PASSWORD | root123 | 数据库密码 |
| SPRING_REDIS_HOST | redis | Redis 地址 |
| SPRING_REDIS_PORT | 6379 | Redis 端口 |
| MINIO_ENDPOINT | http://minio:9000 | MinIO 地址 |
| MINIO_ACCESS_KEY | minioadmin | MinIO 用户名 |
| MINIO_SECRET_KEY | minioadmin | MinIO 密码 |

## 数据持久化

MySQL 和 MinIO 的数据通过 Docker Volume 持久化：

- `mysql-data` → MySQL 数据目录
- `minio-data` → MinIO 数据目录

数据不会因容器重启而丢失。

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看应用日志
docker-compose logs -f app

# 停止所有服务
docker-compose down

# 停止并删除数据卷（慎用，会丢失数据）
docker-compose down -v

# 重新构建应用镜像
docker-compose build app
```

## 首次启动注意事项

首次启动时 MySQL 需要执行初始化脚本（sql/langhaiblogs.sql），应用服务会等待 MySQL 健康检查通过后才启动。如果应用启动后连接数据库失败，请等待约 30 秒后重启应用：

```bash
docker-compose restart app
```
