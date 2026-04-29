# 校园宣讲会预约与签到系统 (Campus Presentation Reservation and Check-in System) Code Wiki

## 1. 项目整体架构
本项目是一个面向校园的**宣讲会预约与签到系统**，采用前后端分离架构。后端基于 Java 17 和 Spring Boot 3.4 构建，前端基于 Vue 3 并在后端的 `src/main/resources/frontend` 目录下独立管理。

### 1.1 技术栈选型
**后端 (Backend)**:
- **核心框架**: Spring Boot 3.4.3
- **持久层**: MyBatis-Plus 3.5.10.1 (MySQL Connector)
- **安全与权限**: Spring Security (使用 BCrypt 加密)
- **缓存与中间件**: Spring Data Redis
- **第三方云服务**: 阿里云 Facebody (人脸识别比对), 阿里云 OSS (对象存储)
- **其他组件**: Lombok, Apache POI (Excel导出/处理)

**前端 (Frontend)**:
- **核心框架**: Vue 3.5.30, Vite 8.0.1
- **UI 组件库**: Element Plus 2.13.6 (@element-plus/icons-vue)
- **状态与路由**: Pinia 3.0.4, Vue Router 5.0.4
- **网络与可视化**: Axios 1.13.6, Echarts 6.0.0

---

## 2. 主要模块职责与目录结构

### 2.1 后端目录 (`src/main/java/com/iflytek/reservation`)
- **`common`**: 公共工具类与基础响应封装 (如 `Result.java`, `AuthTokenUtil.java`, `IndustryUtil.java`)。
- **`config`**: 系统配置类。包括 MyBatis-Plus 分页插件 (`MybatisPlusConfig.java`) 和基于 Spring Security 的认证安全策略 (`SecurityConfig.java`)。
- **`controller`**: 控制层，处理各角色的 HTTP 请求。划分为管理员、企业、学生三类 Controller (如 `AdminDashboardController.java`, `StudentReservationController.java` 等) 以及统一认证入口 (`LoginController.java`)。
- **`entity`**: 数据库实体类映射 (如 `Admin`, `Company`, `Student`, `Session`, `Reservation`, `Checkin`, `FaceData`)。
- **`integration/aliyun`**: 阿里云服务的集成与封装，主要通过 `AliyunClients.java` 提供单例的 Facebody 与 OSS 客户端支持。
- **`job`**: 定时任务模块 (如 `SessionArchiveJob.java` 用于宣讲会状态定时归档)。
- **`mapper`**: MyBatis-Plus 数据访问层接口。
- **`service` / `service.impl`**: 核心业务逻辑层。处理复杂的预约、签到、人脸注册搜索等核心业务。

### 2.2 前端目录 (`src/main/resources/frontend/src`)
- **`api`**: 按角色划分的 Axios 请求接口 (如 `admin.js`, `auth.js`, `student.js`)。
- **`views`**: 页面视图组件。划分为 `admin`, `company`, `student`, `checkin` 以及公用 `Dashboard.vue` 等。
- **`components` & `layout`**: 通用组件和基础布局 (`StandardLayout.vue`, `BlankLayout.vue`)。
- **`router` & `store`**: Vue Router 路由守卫与权限控制，Pinia 全局状态管理 (如 `user.js` 管理用户角色与 Token)。
- **`utils`**: 前端工具类，如请求拦截器 `request.js`。

---

## 3. 关键类与函数说明

### 3.1 核心后端类
- **`ReservationApplication.java`**: Spring Boot 主启动类，启用了 `@EnableScheduling` 定时任务与 `@MapperScan` Mapper 扫描。
- **`AliyunClients.java`**:
  - **职责**: 管理和初始化阿里云 OSS 与 Facebody 人脸识别服务的客户端连接。
  - **关键函数**: `faceClient()` 和 `ossClient()`，利用双重检查锁实现客户端实例的单例化并获取环境变量 `ALIBABA_CLOUD_ACCESS_KEY_ID`。
- **`LoginController.java`**:
  - **职责**: 统一的登录入口与路由分发。
  - **关键函数**: `login()` 根据角色类型 (STUDENT, COMPANY, ADMIN) 调用对应的 Service 进行校验，并返回响应。
- **`AdminAccountController.java`**:
  - **职责**: 管理员操作接口，如分页查询其他管理员或企业状态等。
  - **关键机制**: 依赖 `AuthTokenUtil.extractId(request)` 解析当前登录用户信息，进行 RBAC 角色鉴权校验。
- **`SecurityConfig.java`**:
  - **职责**: 全局安全配置。
  - **关键配置**: `passwordEncoder()` 注入 `BCryptPasswordEncoder` 实例，提供哈希密码校验功能。

### 3.2 核心前端机制
- **`request.js`**:
  - 封装 Axios 请求，添加请求拦截器(Request Interceptor)用于携带 Token，响应拦截器(Response Interceptor)用于统一处理错误及会话过期。
- **`user.js` (Pinia Store)**:
  - 管理当前登录用户的基本信息、角色以及 Token，并持久化到 localStorage 中。

---

## 4. 核心依赖关系
本项目的核心数据流向为：**前端 Vue 视图层** -> **Axios (API)** -> **Spring Boot Controller** -> **Service 业务逻辑** -> **Mapper / 阿里云云服务** -> **MySQL / OSS / Facebody**。

### 4.1 数据库 ER 模型依赖 (详见 `reservation.sql`)
- **核心业务流**：
  - `company` 发布 `session` (宣讲会)。
  - `student` 通过预约形成 `reservation` (预约记录)。
  - `student` 上传底片并提取特征保存到 `face_data`。
  - `student` 现场签到产生 `checkin` (签到记录)，同时校验对应 `session` 和 `reservation` 的状态。

---

## 5. 项目运行方式

### 5.1 环境要求
- JDK 17 及以上
- Node.js 18+ (推荐使用 `pnpm`)
- MySQL 8.0+
- Redis (默认 6379 端口)
- 阿里云账号 (需要开通 Facebody 人脸人体与 OSS 对象存储服务)

### 5.2 配置修改
1. **环境变量配置**: 
   由于涉及到云服务，运行前需要设置以下环境变量：
   - `ALIBABA_CLOUD_ACCESS_KEY_ID`
   - `ALIBABA_CLOUD_ACCESS_KEY_SECRET`
   - `RESERVATION_OSS_BUCKET` (可选)
   - `RESERVATION_OSS_ENDPOINT` (可选)
2. **数据库与 Redis 配置**:
   在 `src/main/resources/application.yaml` 中修改 `datasource` 的 `username` / `password` 以及 Redis 连接信息。

### 5.3 启动步骤
1. **数据库初始化**: 
   将 `src/main/resources/db/reservation.sql` 导入到 MySQL 数据库 `reservation` 中。
2. **启动后端服务**: 
   在 IDE 中运行 `ReservationApplication.java` 或在项目根目录下执行：
   ```bash
   ./mvnw spring-boot:run
   ```
3. **启动前端服务**:
   前端工程独立于后端静态资源目录中，需进入对应目录启动：
   ```bash
   cd src/main/resources/frontend
   pnpm install
   pnpm run dev
   ```
4. **访问**: 前端启动后，通过浏览器访问 Vite 提供的本地端口 (通常为 `http://localhost:5173`) 进行测试与调试。