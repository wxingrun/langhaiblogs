浪海博客   
[english](./README-EN.md)  

简介：
浪海博客，一个基于SpringBoot快速构建的单体架构项目，部署简单方便，适用于个人博客系统搭建。

在线演示：http://www.langhai.cc  
实际效果以代码为准（演示站点是以前的版本） 联系方式QQ：676558206 技术交流QQ群 585975304  
分支说明：master是完整版本，simpleness分支只需要数据库mysql和redis以及文件存储组件minio就可以启动，无需引入搜索引擎es和消息队列mq。

部分页面截图    
首页页面截图
![首页截图](./images/首页截图.png)  
登录页面截图
![登录页面截图](./images/登录页面截图.png)

部署方式：[详细说明](https://langhai.cc/article/articleShow?id=38)  
linux ==>> nohup java -jar langhai-blogs.jar > langhai.log &  
windows ==>> java -jar langhai-blogs.jar

技术选型：  
springboot 后端快速构建框架  
thymeleaf 数据模板引擎  
hutool java工具集  
netty websocket flowable  
redis minio elasticSearch rabbitMQ

版权/引用声明  
浪海博客系统的主要前端模板来自html5up.net网站。  
博客前端模板 [燕十三博客模板](https://gitee.com/yssgit/yan_shisan_blog_template)  
导航模块来自开源项目 [geekape](https://github.com/geekape/geek-navigation)    
后台管理来自开源项目 [Pear Admin](https://gitee.com/pear-admin/Pear-Admin-Layui)  
聚合搜索来自开源项目 [juso](https://github.com/yitd/juso.vip)  
音乐播放器来自开源项目 [QPlayer](https://github.com/Jrohy/QPlayer)

基本组件：  
关系型数据库 MySQL [MySQL 详细说明](http://www.langhai.cc/article/articleShow?id=53) 

```sql
/* 
	导入langhaiblogs/sql/langhaiblogs.sql之后需要填充默认数据。
	新增角色 注意和代码保持一致 cc.langhai.config.constant.RoleConstant
*/
INSERT INTO role VALUES(1, 'admin', NOW(), NULL);
INSERT INTO role VALUES(2, 'user', NOW(), NULL);
INSERT INTO role VALUES(3, 'vip', NOW(), NULL);

```

非关系型数据库 Redis  
图片存储服务器 minio [minio 详细说明](http://www.langhai.cc/article/articleShow?id=54)   
搜索引擎(可选) elasticSearch [elasticSearch 相关说明](http://www.langhai.cc/article/articleShow?id=55)   
消息队列（可选）rabbitMQ   <a href="https://langhai.cc/article/articleShow?id=33">rabbitMQ所有说明</a>

---

### Docker 容器化环境配置（Trae/Docker rollout 支持）

本项目已支持通过 Docker 容器化环境进行基础构建和调试。容器内置了 JDK 8 和 Maven 环境，并启动了 SSH 守护进程（OpenSSH Server），允许 Trae 通过 SSH 直接进入容器内部进行调试。

**基础构建与完整运行的区别：**
- **基础构建**：通过 `Dockerfile` 构建的容器仅提供代码的编译、打包环境（如 `mvn clean package -DskipTests`）。容器内部没有安装 MySQL、Redis、Elasticsearch、RabbitMQ、MinIO 等外部服务。此时运行 Spring Boot 会因为缺少外部服务而启动失败，但容器会保持存活以供 SSH 调试。
- **完整运行**：需要依赖完整的外部服务组件（MySQL, Redis, MinIO, ES, RabbitMQ）。若需完整运行，请修改 `application.yml` 连接到实际运行的这些服务（例如通过 Docker Compose 或者外部独立部署的服务）。

**构建和运行 Docker 环境命令：**

1. 构建镜像：
```bash
docker build -t langhaiblogs-env .
```

2. 运行容器：
```bash
docker run -d -p 2222:22 -p 2086:2086 --name langhaiblogs-container langhaiblogs-env
```

3. 验证连接：
- 确认容器内目录为 `/app`： `docker exec langhaiblogs-container pwd`
- 验证 SSH 服务可连接： `ssh root@localhost -p 2222` （如果提示密码，默认环境配置了免密或需配置 authorized_keys，但当前镜像允许 root 登录）
