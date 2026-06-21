# 双轨笔记本 — 部署文档

> 项目：two-track-notebook  
> 技术栈：Spring Boot 3.5 + MyBatis-Plus + Sa-Token JWT + MySQL 8.0 + Uni-app Vue3 + Vite  
> 更新日期：2026-06-02

---

## 目录

1. [架构概览](#1-架构概览)
2. [后端部署（Railway）](#2-后端部署railway)
3. [前端部署（Vercel）](#3-前端部署vercel)
4. [本地开发环境](#4-本地开发环境)
5. [内网穿透（NATAPP 给老师演示用）](#5-内网穿透natapp-给老师演示用)
6. [常见问题](#6-常见问题)

---

## 1. 架构概览

```
┌─────────────────┐     HTTPS      ┌──────────────────────┐
│   浏览器/手机                                    │
│  Vercel 前端 (H5)  ─────────►  Railway 后端       │
│  two-track-notebook.vercel.app  │  Spring Boot 8080   │
│                         │  MySQL on Railway   │
└─────────────────┘     JWT Token     └──────────────────────┘
```

| 组件 | 部署平台 | 地址示例 |
|------|----------|----------|
| 前端（Uni-app H5） | Vercel | `https://two-track-notebook.vercel.app` |
| 后端（Spring Boot） | Railway | `https://two-track-notebook-production.up.railway.app` |
| 数据库（MySQL 8.0） | Railway | 内部网络，不对外暴露 |

---

## 2. 后端部署（Railway）

### 2.1 准备工作

1. 注册 [Railway](https://railway.app) 账号
2. 安装 Railway CLI（可选）：`npm install -g @railway/cli`
3. 准备 MySQL 数据库（Railway 提供免费 MySQL 插件）

### 2.2 通过 GitHub 集成部署（推荐）

1. 在 Railway 控制台点击 **"New Project"** → **"Deploy from GitHub repo"**
2. 授权 Railway 访问你的 Gitee/GitHub 仓库
3. 选择仓库：`two_track_notebook`
4. 设置 Root Directory：`backend`
5. Railway 会自动检测 `Dockerfile` 并构建

### 2.3 环境变量配置

在 Railway 项目 → **Variables** 中添加：

```properties
# Spring 环境
SPRING_PROFILES_ACTIVE=railway

# 数据库（Railway MySQL 插件自动注入，也可手动填写）
DB_URL=jdbc:mysql://mysql:3306/two_track_notebook?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=xxxxxx

# JWT 密钥（和 application-railway.properties 一致）
JWT_SECRET=two-track-notebook-secret-key-2026

# Sa-Token 配置
SA_TOKEN_TIMEOUT=2592000
```

### 2.4 `application-railway.properties` 配置

```properties
# ===========================================
# Railway 生产环境配置
# 激活方式：SPRING_PROFILES_ACTIVE=railway
# ===========================================

# 数据源（使用 Railway 内部 MySQL）
spring.datasource.url=${DB_URL:jdbc:mysql://mysql:3306/two_track_notebook?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:123456}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Sa-Token JWT 配置（与本地一致）
sa-token.token-name=Authorization
sa-token.timeout=2592000
sa-token.active-timeout=-1
sa-token.is-concurrent=true
sa-token.token-style=jwt-simple
sa-token.jwt-secret-key=two-track-notebook-secret-key-2026

# 服务器配置
server.port=8080
server.servlet.context-path=/api

# 日志
logging.level.com.twotrack=INFO
```

### 2.5 确认 CORS 配置

`WebConfig.java` 必须存在，否则前端无法跨域访问：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
```

### 2.6 部署验证

```bash
# 健康检查（如果添加了 Actuator）
curl https://two-track-notebook-production.up.railway.app/api/actuator/health

# 或测试验证码接口
curl https://two-track-notebook-production.up.railway.app/api/auth/captcha
```

---

## 3. 前端部署（Vercel）

### 3.1 准备工作

1. 注册 [Vercel](https://vercel.com) 账号
2. 安装 Vercel CLI（可选）：`npm i -g vercel`

### 3.2 通过 GitHub 集成部署（推荐）

1. 在 Vercel 控制台点击 **"New Project"**
2. 导入你的 Git 仓库
3. 配置如下：

| 配置项 | 值 |
|--------|-----|
| Framework Preset | `Vite` |
| Root Directory | `frontend` |
| Build Command | `npm install && npm run build:h5` |
| Output Directory | `dist/build/h5` |
| Install Command | `npm install` |

4. 点击 **Deploy**，等待构建完成

### 3.3 环境变量配置

在 Vercel 项目 → **Settings** → **Environment Variables** 中添加：

```
# 如果前端需要内嵌后端地址（可选，通常在 request.ts 里硬编码）
VITE_API_BASE_URL=https://two-track-notebook-production.up.railway.app/api
```

### 3.4 `request.ts` 生产环境配置

`frontend/src/utils/request.ts`：

```typescript
// 开发模式走 Vite 代理，生产环境直接用 Railway 后端地址
const isDev = import.meta.env.DEV
const BASE_URL = isDev 
  ? '/api' 
  : 'https://two-track-notebook-production.up.railway.app/api'
```

### 3.5 自定义域名（可选）

1. Vercel 项目 → **Settings** → **Domains**
2. 添加你的域名（如 `notebook.yourdomain.com`）
3. 按提示在域名 DNS 添加 Vercel 提供的 CNAME 记录

### 3.6 中国访问问题

> ⚠️ **注意**：`*.vercel.app` 域名在中国大陆可能被屏蔽。  
> **解决方案**：
> - 绑定自定义域名（国内备案域名）
> - 或使用内网穿透（NATAPP）给老师/评委演示

---

## 4. 本地开发环境

### 4.1 后端启动

```bash
# 前提：MySQL 本地运行在 3306 端口，数据库名 two_track_notebook

cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

访问：`http://localhost:8080/api/auth/captcha`

### 4.2 前端启动

```bash
cd frontend
npm install
npm run dev:h5
```

访问：`http://localhost:5173/#/`

> Vite 代理已配置：`/api` → `http://127.0.0.1:8080`（见 `vite.config.ts`）

### 4.3 本地数据库初始化

```bash
mysql -u root -p123456 < sql/init.sql
```

---

## 5. 内网穿透（NATAPP，给老师演示用）

> 适用场景：老师/评委在国内，无法直接访问 `*.vercel.app` 和 `*.up.railway.app`

### 5.1 整体方案

```
老师浏览器
    │
    ▼
NATAPP 免费隧道（域名：xxx.natappfree.cc）
    │
    ▼
你的电脑（前端静态文件，端口 3000）
    │  API 请求
    ▼
Railway 后端（生产环境，不需要穿透）
```

| 组件 | 穿透方式 | 说明 |
|------|----------|------|
| 前端（静态文件） | NATAPP 免费隧道 | 穿透到本地 3000 端口 |
| 后端 | **不需要穿透** | 直接使用 Railway 生产地址 |

### 5.2 NATAPP 配置步骤

**第一步：注册并开通隧道**

1. 访问 [NATAPP 官网](https://natapp.cn) 注册
2. 「购买隧道」→ 选择「免费隧道」
3. 配置：
   - 协议：`Web`
   - 本地地址：`127.0.0.1`
   - 本地端口：`3000`（前端静态文件服务端口）

**第二步：下载客户端**

- Windows：下载 `natapp.exe`
- 放在任意目录，如 `C:\natapp\`

**第三步：配置 `config.ini`**

在 `natapp.exe` 同目录下创建 `config.ini`：

```ini
[default]
authtoken=你的免费隧道authtoken
clienttoken=
log=stdout
loglevel=DEBUG
http_proxy=
```

**第四步：启动 NATAPP**

```bash
cd C:\natapp
natapp -config=config.ini
```

启动成功后，日志会显示：

```
Tunnel established at: http://xxxx.natappfree.cc
Forwarding: http://xxxx.natappfree.cc -> 127.0.0.1:3000
```

### 5.3 前端静态文件托管

NATAPP 需要访问本地的静态文件服务（不是 Vite dev server）。

**方案 A：build 后用 `serve` 托管（推荐）**

```bash
cd frontend
npm run build:h5
npx serve -l 3000 dist/build/h5
```

**方案 B：用 `http-server`**

```bash
cd frontend
npm run build:h5
npx http-server dist/build/h5 -p 3000
```

### 5.4 修改前端 API 地址

`frontend/src/utils/request.ts`：

```typescript
const BASE_URL = 'https://two-track-notebook-production.up.railway.app/api'
```

> 这样前端页面通过 NATAPP 访问，但 API 请求直接发到 Railway 后端（不需要穿透后端）。

### 5.5 给老师分享

直接把 NATAPP 域名发给老师：

```
http://xxxx.natappfree.cc/#/
```

> ⚠️ 免费隧道域名 **8 小时后失效**，需要重新开通。  
> 如果需要长期稳定，购买 VIP 隧道 + 二级域名（约 ¥15/年）。

---

## 6. 常见问题

### Q1：前端访问后端提示 CORS 错误

**原因**：后端未配置 `WebConfig.java`，或 `allowedMethods` 缺少 `PATCH`。

**解决**：确认 `WebConfig.java` 已实现 `addCorsMappings`，且包含 `PATCH` 方法。

---

### Q2：登录后立刻跳回登录页（登录循环）

**原因**：`application.yml` 和 `application-railway.properties` 中 `sa-token.token-name` 不一致。

**解决**：两个文件都设置为 `Authorization`：

```properties
sa-token.token-name=Authorization
```

---

### Q3：Vercel 部署后访问 404

**原因**：`vercel.json` 配置了 `"builds"` 字段，导致 Vercel 使用 Build Output API 格式，但项目并未按此格式输出。

**解决**：删除 `frontend/vercel.json`，让 Vercel 使用 UI 中的配置（Output Directory = `dist/build/h5`）。

---

### Q4：NATAPP 启动后访问显示 `Tunnel xxx not found`

**原因**：免费隧道域名已过期（8小时有效期）。

**解决**：
1. 登录 NATAPP 后台删除旧隧道
2. 重新开通免费隧道
3. 更新 `config.ini` 中的 `authtoken`
4. 重启 NATAPP 客户端

---

### Q5：老师无法访问 `*.vercel.app` 域名

**原因**：中国大陆网络环境屏蔽了部分境外域名。

**解决**：
- 使用 NATAPP 内网穿透（见第 5 节）
- 或为 Vercel 项目绑定已备案的自定义域名

---

### Q6：消息发送失败，后端报外键约束错误

**原因**：前端发送 `personaId: 0` 表示"用户自己"，但 `persona_config` 表没有 `id=0` 的记录。

**解决**：`frontend/src/pages/explore/detail.vue` 中，将 `personaId: 0` 改为 `personaId: undefined`：

```typescript
personaId: currentPersonaId.value === 0 
  ? undefined 
  : (currentPersonaId.value || undefined)
```

---

## 附录 A：完整环境变量清单

### 后端（Railway）

| 变量名 | 示例值 | 说明 |
|--------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `railway` | 激活 Railway 环境配置 |
| `DB_URL` | `jdbc:mysql://mysql:3306/...` | 数据库连接地址 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `xxxx` | 数据库密码 |
| `JWT_SECRET` | `two-track-notebook-secret-key-2026` | JWT 签名密钥 |

### 前端（Vercel）

| 变量名 | 示例值 | 说明 |
|--------|--------|------|
| `VITE_API_BASE_URL` | `https://xxx.up.railway.app/api` | 后端 API 地址（可选） |

---

*文档由 WorkBuddy AI 协助生成，最后更新：2026-06-02*
