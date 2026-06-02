# 浪海博客 Docker 部署说明

## 一键启动项目

### 前置要求
- Docker 和 Docker Compose 已安装
- 项目已使用 Maven 编译打包（执行 `mvn clean package`）

### 快速启动
```bash
# 1. 编译打包项目
mvn clean package -DskipTests

# 2. 一键启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看应用日志
docker-compose logs -f app
```

## 服务说明

| 服务 | 访问地址 | 说明 |
|------|---------|------|
| 博客应用 | http://localhost:2086 | 浪海博客主页 |
| MinIO 控制台 | http://localhost:9001 | 存储管理控制台（账号：minioadmin / 密码：minioadmin） |
| MySQL | localhost:3306 | 数据库（库名：langhaiblogs，账号：root / 密码：root） |
| Redis | localhost:6379 | 缓存（DB: 9，无密码） |

## 数据持久化

所有数据会持久化到 Docker 本地卷中，即使容器重启数据也不会丢失：
- MySQL 数据：`langhaiblogs_mysql-data`
- Redis 数据：`langhaiblogs_redis-data`
- MinIO 数据：`langhaiblogs_minio-data`

## 常用命令

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（慎用！会清空所有数据）
docker-compose down -v

# 重新构建并启动应用
docker-compose up -d --build

# 查看所有服务日志
docker-compose logs -f
```

## 配置说明

可以通过环境变量覆盖默认配置，在 docker-compose.yml 中添加 `environment` 配置项即可。
