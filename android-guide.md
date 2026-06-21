# 双轨笔记本 - Android 打包指南

## 项目概述

- **前端框架**：Uni-app (Vue3 + Vite)
- **编译输出**：`dist/build/app`
- **后端**：已部署在 Railway (`https://two-track-notebook-production.up.railway.app/api`)
- **Android 配置**：已自动处理

---

## 已完成的配置

### 1. `manifest.json` - 已添加 Android 权限

```json
"app-plus": {
  "distribute": {
    "android": {
      "permissions": [
        "<uses-permission android:name=\"android.permission.INTERNET\" />",
        "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />"
      ]
    }
  }
}
```

### 2. `request.ts` - App端自动连接生产环境

```typescript
// #ifdef APP-PLUS
// App端（Android/iOS）连接生产环境后端
BASE_URL = 'https://two-track-notebook-production.up.railway.app/api'
// #endif
```

App 端自动指向 Railway 后端，**不需要本地启动后端**。

---

## 打包方法（二选一）

### 方案A：HBuilderX 云打包（推荐，最简单）

**步骤：**

1. **下载安装 HBuilderX**
   - 官网：https://www.dcloud.io/hbuilderx.html

2. **导入项目**
   - HBuilderX → 文件 → 导入 → 从本地目录导入
   - 选择：`C:/Users/ren/WorkBuddy/2026-05-25-10-18-15/two_track_notebook/frontend`

3. **选择运行方式**
   - HBuilderX → 运行 → 运行到手机或模拟器 → 制作App资源
   - 或者直接使用已编译好的 `dist/build/app`

4. **云打包**
   - HBuilderX → 发行 → 原生App-云打包
   - 选择 **Android (apk)**
   - 勾选 **使用公共测试证书**（或上传自己的证书）
   - 点击 **打包**
   - 等待 DCloud 云端打包完成（约 2-5 分钟）
   - 下载 APK 文件

5. **安装测试**
   - 把 APK 传到 Android 手机
   - 点击安装（可能需要允许"未知来源应用"）
   - 打开 App 测试

---

### 方案B：Android Studio 本地打包（高级）

如果你需要自定义更多原生功能：

1. **下载 Android Studio**
   - 官网：https://developer.android.com/studio

2. **生成本地 App 资源**
   ```bash
   cd C:/Users/ren/WorkBuddy/2026-05-25-10-18-15/two_track_notebook/frontend
   npx uni build -p app-android
   ```

3. **使用 HBuilderX 生成本地打包 App**
   - HBuilderX → 发行 → 原生App-本地打包 → 生成本地打包 App 资源
   - 会生成 `unpackage/resources/__UNI__XXXXXXX` 目录

4. **在 Android Studio 中打开**
   - 打开 HBuilderX 提供的 Android 原生工程模板
   - 将 App 资源拷贝到 `assets/apps/` 目录
   - 构建 APK

---

## 安装后测试清单

| 功能 | 测试步骤 | 预期结果 |
|------|---------|---------|
| 启动 | 点击 App 图标 | 显示启动页，进入登录页 |
| 注册 | 输入用户名密码，点击注册 | 提示注册成功，自动登录 |
| 登录 | 输入账号密码 | 进入现实轨首页，显示任务列表 |
| 现实轨 | 创建、编辑、删除任务 | 操作成功，列表刷新 |
| 探索轨 | 创建议题，发送消息 | 消息显示正常，AI 可回复（如果有配置） |
| 原则库 | 查看原则列表，添加原则 | 原则正常显示 |
| 注意力审计 | 开始心流、记录时间 | 数据正常保存 |
| 我的 | 查看个人信息 | 信息正确显示 |

---

## ⚠️ 重要提醒

### 1. 后端域名可访问性

App 连接的是 `two-track-notebook-production.up.railway.app`：

| 场景 | 状态 |
|------|------|
| 手机在国内 + 无特殊网络 | 可能无法访问（`.railway.app` 域名被墙） |
| 手机在国内 + 使用 VPN | ✅ 可以访问 |
| 手机在国外 | ✅ 可以访问 |

**如果无法访问，需要：**
- 使用 VPN
- 或者将后端部署到国内服务器（阿里云/腾讯云）
- 或者修改 `request.ts` 里的 `BASE_URL` 为 NATAPP 穿透地址（临时演示）

### 2. 修改 App 后端地址（临时调试）

如果需要连本地后端调试：

```typescript
// 打开 frontend/src/utils/request.ts
// #ifdef APP-PLUS
// 临时改成你的 NATAPP 地址或本地 IP
BASE_URL = 'http://你的natapp域名/api'
// #endif
```

修改后需要重新编译：
```bash
npx uni build -p app-android
```

### 3. HTTPS 证书

Railway 默认使用 HTTPS，如果自建后端需要确保：
- 域名有有效 SSL 证书
- 或者关闭证书校验（仅开发调试，不推荐）

---

## 常见问题

### Q1: HBuilderX 云打包提示"没有 App 资源"

**解决：**
1. 先执行 `npx uni build -p app-android`
2. 在 HBuilderX 里右键项目 → 发行 → 原生App-云打包

### Q2: 安装后打开白屏

**排查：**
1. 检查手机网络是否正常
2. 检查后端域名是否可访问（在浏览器打开 `https://two-track-notebook-production.up.railway.app/api/auth/captcha`）
3. 检查 `manifest.json` 的 `app-plus` 配置是否正确

### Q3: 提示"网络异常，请检查后端是否启动"

**排查：**
1. 确认手机能访问外网
2. 确认后端域名在国内可访问（可能需要 VPN）
3. 在 `request.ts` 的 `fail` 回调里加 `console.log(JSON.stringify(err))` 查看详细错误

### Q4: 如何生成正式签名的 APK

**步骤：**
1. 生成 keystore 文件：
   ```bash
   keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
   ```
2. 在 HBuilderX 云打包时上传 keystore
3. 或本地打包时使用 Android Studio 签名

---

## 快速操作总结

```bash
# 1. 编译 App 资源
cd C:/Users/ren/WorkBuddy/2026-05-25-10-18-15/two_track_notebook/frontend
npx uni build -p app-android

# 2. 用 HBuilderX 打开项目，发行 → 原生App-云打包
# 3. 下载 APK，安装到手机
# 4. 测试功能
```

---

## 相关文档

- [部署指南](deploy-guide.md) - 后端/前端部署说明
- [测试用例](test-cases.md) - 后端接口测试用例
- [小程序指南](mp-weixin-guide.md) - 微信小程序部署
