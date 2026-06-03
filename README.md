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

### Docker 部署（推荐用于 Trae / 开发调试）

**基础构建 vs 完整运行的差异说明：**

| 运行模式 | 需要的外部服务 | 说明 |
|---------|---------------|------|
| 基础构建 (`docker build`) | **无** | 仅依赖 Maven 中央仓库，完成 `mvn package -DskipTests` |
| 完整运行 (`java -jar app.jar`) | MySQL + Redis + MinIO + ElasticSearch + RabbitMQ | 所有外部服务就绪后应用才能正常启动 |

Docker 镜像构建命令：
```bash
docker build -t langhaiblogs:dev .
```

Docker 容器运行命令（映射 SSH 到 2222 端口）：
```bash
docker run -d --name langhaiblogs-dev -p 2222:22 -p 2086:2086 langhaiblogs:dev
```

SSH 连接容器（root 用户，密码 root）：
```bash
ssh -p 2222 root@localhost
```

确认容器内目录为 /app：
```bash
ssh -p 2222 root@localhost "pwd && ls -la /app"
```

确认 SSH 服务可连接：
```bash
ssh -p 2222 root@localhost "echo SSH OK: $(date)"
```

**注意：** Dockerfile 中的 root 密码仅用于开发调试环境，不可用于生产。生产环境请使用 SSH Key 认证。

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