# 浪海博客系统 - 容器化部署指南

本项目已添加完整的 Docker 容器化支持，能够通过 Docker Compose 一键启动所有依赖服务和应用本体。

## 前置环境要求

- 安装 Docker
- 安装 Docker Compose

## 一键启动步骤

1. 在项目根目录（即 `docker-compose.yml` 所在目录）下打开终端。
2. 执行以下命令：

   ```bash
   docker-compose up -d --build
   ```

   *说明：`--build` 参数确保每次都能根据最新的代码和配置重新构建应用镜像。*

3. 等待所有容器启动并进入健康状态。可以使用以下命令查看运行日志：

   ```bash
   docker-compose logs -f
   ```

4. 启动完成后，在浏览器中访问：[http://localhost:2086](http://localhost:2086) 即可看到博客首页。

## 服务说明

`docker-compose.yml` 包含以下服务：

- **mysql**: 运行 MySQL 8.0。默认端口 `3306`，会自动创建 `langhaiblogs` 数据库，并执行 `sql/langhaiblogs.sql` 初始化表结构。数据会持久化到本地 `mysql_data` 数据卷。
- **redis**: 运行 Redis 6.2。默认端口 `6379`。
- **minio**: 运行 MinIO (RELEASE.2023-09-07T02-05-02Z)。API 端口 `9000`，控制台端口 `9001`。默认账号/密码均为 `minioadmin`。数据持久化到本地 `minio_data` 数据卷。
- **app**: 博客应用服务端，基于 `openjdk:8-jre-slim` 构建。依赖上述组件正常启动。

## 配置说明

我们在 `application.yml` 中修改了数据源、Redis 和 MinIO 的配置，改为通过环境变量注入，并配置了默认值以匹配 Docker Compose 里的服务名。例如：
- 数据库连接：`${MYSQL_HOST:mysql}:${MYSQL_PORT:3306}`
- Redis 地址：`${REDIS_HOST:redis}`
- MinIO 地址：`${MINIO_ENDPOINT:http://minio:9000}`

如果你需要覆盖这些配置（例如在其他部署环境中），可以直接在部署时注入对应的环境变量。

## 手动验证步骤

1. **验证容器状态**：执行 `docker-compose ps` 确认所有容器均为 `Up` 状态（MySQL 需要显示 `healthy`）。
2. **验证数据卷持久化**：执行 `docker volume ls`，确认生成了 `langhaiblogs_mysql_data` 和 `langhaiblogs_minio_data` 卷。
3. **验证博客访问**：浏览器访问 `http://localhost:2086`。
4. **验证数据库初始化**：可以使用数据库客户端连接 `localhost:3306`（密码 `root`），查看 `langhaiblogs` 数据库及其中表是否被正确创建。