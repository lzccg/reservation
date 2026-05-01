# 部署文档（reservation）

本仓库包含一个 Spring Boot 后端（单体 API 服务）和一个 Vite + Vue3 前端（独立静态站点）。仓库内未提供 Docker/Compose/K8s/CI 编排文件，本文提供可落地的部署方式与配置清单。

## 1. 项目结构与组件

- 后端：Java 17 + Spring Boot 3.4.3 + MyBatis-Plus（[pom.xml](file:///workspace/reservation/pom.xml)）
  - 启动类：[ReservationApplication](file:///workspace/reservation/src/main/java/com/iflytek/reservation/ReservationApplication.java)
  - API 路由前缀：`/api/*`（控制器位于 [controller](file:///workspace/reservation/src/main/java/com/iflytek/reservation/controller)）
- 数据存储：MySQL（必需），Redis（配置存在但代码侧未发现硬依赖）
  - Spring Boot 配置：[application.yaml](file:///workspace/reservation/src/main/resources/application.yaml)
  - 初始化 SQL：[reservation.sql](file:///workspace/reservation/src/main/resources/db/reservation.sql)
- 三方依赖：阿里云 Facebody（人脸）与 OSS（图片/底片存储）
  - 客户端工厂：[AliyunClients](file:///workspace/reservation/src/main/java/com/iflytek/reservation/integration/aliyun/AliyunClients.java)
- 前端：Vite + Vue 3 + Element-Plus + Pinia + Axios（[frontend/package.json](file:///workspace/reservation/src/main/resources/frontend/package.json)）
  - 开发代理：`/api -> http://localhost:8080`（[vite.config.js](file:///workspace/reservation/src/main/resources/frontend/vite.config.js)）

## 2. 环境与依赖

### 2.1 运行时依赖

- Java：建议 Java 17（最低），生产推荐使用 LTS（17/21）并与编译目标一致
- MySQL：8.x
- Redis：可选（若启用相关能力再接入）

### 2.2 构建时依赖

- Maven：仓库提供 Maven Wrapper（`mvnw`），在某些环境下需要用 `bash ./mvnw ...` 执行
- Node.js：建议 20+（本仓库前端依赖较新）
- pnpm：前端包管理器（`pnpm -v` 可验证）

### 2.3 网络与制品仓库

后端与前端构建都需要拉取依赖（Maven Central / npm registry）。若部署环境不能直连公网，需要：

- Maven：配置公司内部镜像（`~/.m2/settings.xml`）或私服（Nexus/Artifactory）
- pnpm：配置 npm 镜像源（如企业内网 registry）

## 3. 配置与环境变量

### 3.1 后端配置（Spring Boot）

默认配置位于 [application.yaml](file:///workspace/reservation/src/main/resources/application.yaml)，包含：

- `server.port`：默认 `8080`
- `spring.datasource.*`：默认指向 `jdbc:mysql://localhost:3306/reservation`，并内置示例账号密码
- `spring.data.redis.*`：默认 `localhost:6379`

生产建议不要改仓库内 `application.yaml`，而是通过以下方式覆盖：

- 方式 A：环境变量覆盖（推荐）
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` / `SPRING_DATA_REDIS_DATABASE`
  - `SERVER_PORT`
- 方式 B：外置配置文件
  - 通过启动参数指定：`--spring.config.location=/etc/reservation/application-prod.yaml`

### 3.2 阿里云配置（人脸/OSS）

代码通过环境变量读取（[AliyunClients](file:///workspace/reservation/src/main/java/com/iflytek/reservation/integration/aliyun/AliyunClients.java)）：

- 必需（未配置会在首次使用相关能力时报错）：
  - `ALIBABA_CLOUD_ACCESS_KEY_ID`
  - `ALIBABA_CLOUD_ACCESS_KEY_SECRET`
- 可选（有默认值）：
  - `RESERVATION_OSS_BUCKET`（默认 `reservation-signin-system`）
  - `RESERVATION_OSS_ENDPOINT`（默认 `https://oss-cn-hangzhou.aliyuncs.com`）

### 3.3 前端配置

前端请求基地址（[request.js](file:///workspace/reservation/src/main/resources/frontend/src/utils/request.js)）：

- `VITE_API_BASE_URL`：默认 `/api`

生产建议让前端与后端同域（例如 Nginx 同域下 `/api` 反代到后端），从而避免跨域与 CORS 配置。

## 4. 数据库初始化

1. 创建数据库（示例）：

```sql
CREATE DATABASE reservation CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. 导入初始化脚本：

```bash
mysql -h <host> -u <user> -p reservation < /path/to/reservation.sql
```

3. 风险提示（强烈建议上线前处理）：

- [reservation.sql](file:///workspace/reservation/src/main/resources/db/reservation.sql) 内包含演示数据与账号
  - 管理员示例：`admin / 123456`（明文）
- 上线前建议：
  - 删除演示数据或仅保留必要字典
  - 重置所有默认账号密码
  - 限制数据库账号权限（最小化授权）

## 5. 部署方式

### 5.1 方式一：前后端分离部署（推荐）

#### 5.1.1 构建后端

在 `reservation/` 目录：

```bash
bash ./mvnw -DskipTests package
```

产物位于：`reservation/target/reservation-0.0.1-SNAPSHOT.jar`

#### 5.1.2 运行后端（示例）

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://<mysql_host>:3306/reservation?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='<mysql_user>'
export SPRING_DATASOURCE_PASSWORD='<mysql_password>'

export ALIBABA_CLOUD_ACCESS_KEY_ID='<ak>'
export ALIBABA_CLOUD_ACCESS_KEY_SECRET='<sk>'
export RESERVATION_OSS_BUCKET='<bucket>'
export RESERVATION_OSS_ENDPOINT='https://oss-<region>.aliyuncs.com'

java -jar reservation-0.0.1-SNAPSHOT.jar
```

#### 5.1.3 构建前端

在 `reservation/src/main/resources/frontend/` 目录：

```bash
pnpm install
pnpm build
```

产物默认位于：`reservation/src/main/resources/frontend/dist/`

#### 5.1.4 使用 Nginx 托管前端并反代后端

建议对外仅暴露 Nginx（80/443），后端 8080 仅在内网可达。

Nginx 示例（按需调整域名/证书/路径）：

```nginx
server {
  listen 80;
  server_name example.com;

  root /var/www/reservation-frontend;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

将 `dist/` 目录内容复制到 `root` 指向的目录即可对外提供前端页面。

### 5.2 方式二：前端打包后随 Jar 一起部署（可选）

该仓库当前未配置“后端直接托管前端静态文件”。如果希望打包成单个后端进程对外提供页面，常见做法是：

1. 前端执行 `pnpm build`；
2. 将 `dist/` 内容复制到后端的 `src/main/resources/static/`；
3. 重新 `mvn package` 打包；
4. 通过 Nginx 仅做反向代理或直接暴露后端端口（不推荐直接暴露）。

## 6. 启动校验

### 6.1 后端端口检查

```bash
curl -sS http://127.0.0.1:8080/ | head
```

该项目未内置独立的健康检查端点（未引入 Actuator），可用业务接口做联通性校验，例如登录接口：

```bash
curl -sS -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456","role":"ADMIN"}'
```

### 6.2 前端联通性检查

- 若前端通过 Nginx 同域部署：打开 `http(s)://<domain>/`，并观察接口请求是否命中 `/api/*`
- 若前后端分离不同域：需要额外配置 CORS（当前后端未见专门 CORS 配置，建议避免跨域部署）

## 7. 安全与上线建议

- 默认安全策略为全放行（[SecurityConfig](file:///workspace/reservation/src/main/java/com/iflytek/reservation/config/SecurityConfig.java) 中 `anyRequest().permitAll()`），上线前建议至少做到：
  - 在网关/Nginx 层限制访问来源（IP 白名单/内网）
  - 启用真实鉴权与权限控制（避免仅依赖前端 token）
- 任何默认账号（如 `admin/123456`）都应在生产环境禁用或强制改密
- 阿里云 AccessKey 不要写入配置文件与镜像，建议通过环境变量或密钥管理系统注入

## 8. 常见问题

- Maven Wrapper 报 “Permission denied”
  - 直接用：`bash ./mvnw ...`（无需给执行权限）
- 构建时无法下载依赖
  - 配置 Maven/npm 内网镜像或允许出网

