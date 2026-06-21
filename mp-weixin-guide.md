# 双轨笔记本 - 微信小程序端部署指南

## 一、编译状态

✅ **编译成功** — 前端代码已完整编译到微信小程序格式

**编译输出目录**：`frontend/dist/build/mp-weixin/`

## 二、准备工作

### 1. 注册微信小程序账号

1. 访问 https://mp.weixin.qq.com
2. 点击「立即注册」→ 选择「小程序」
3. 按提示完成邮箱验证、主体信息登记（个人/企业）
4. 完成注册后，进入「开发」→「开发管理」→「开发设置」
5. 复制 **AppID**（格式如：`wx1234567890abcdef`）

### 2. 下载微信开发者工具

1. 访问 https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
2. 下载对应系统的稳定版 Stable Build
3. 安装并登录（用微信扫码）

### 3. 配置后端合法域名（⚠️ 关键）

在微信小程序后台，必须配置后端 API 域名，否则请求会被拦截：

1. 登录 https://mp.weixin.qq.com
2. 进入「开发」→「开发管理」→「服务器域名」
3. 在 **request 合法域名** 中添加：
   ```
   https://two-track-notebook-production.up.railway.app
   ```
4. 点击「保存并提交」→ 扫码确认

> ⚠️ **重要**：微信小程序只能访问 HTTPS 域名。你的 Railway 后端已使用 HTTPS，但域名在中国大陆可能不稳定。如果测试时发现请求失败，可能需要备选方案（见「常见问题」）。

## 三、配置项目

### 1. 填写 AppID

打开 `frontend/manifest.json`，找到 `mp-weixin` 节点，填入你的小程序 AppID：

```json
{
  "mp-weixin": {
    "appid": "wx1234567890abcdef"
  }
}
```

然后重新编译：
```bash
cd frontend
npm run build:mp-weixin
```

### 2. 导入微信开发者工具

1. 打开微信开发者工具
2. 点击「+」→「导入项目」
3. 选择目录：`frontend/dist/build/mp-weixin`
4. 输入 AppID（选「测试号」也可以临时测试）
5. 点击「确定」

## 四、开发调试

### 预览模式

在开发者工具中点击「预览」，生成二维码，用微信扫码即可在真机上测试。

### 真机调试

点击「真机调试」→ 扫码，可以在手机上实时查看 console 日志。

## 五、发布流程

### 1. 上传代码

在开发者工具中点击「上传」→ 填写版本号（如 `1.0.0`）和项目备注 → 上传。

### 2. 提交审核

1. 登录小程序后台 https://mp.weixin.qq.com
2. 进入「管理」→「版本管理」→「开发版本」
3. 点击「提交审核」
4. 填写功能页面（选择 `pages/login/index` 作为首页）
5. 提交审核（个人小程序一般 1-3 天）

### 3. 发布上线

审核通过后，点击「发布」即可上线。

## 六、常见问题

### Q1: 请求失败，提示 "不在以下 request 合法域名列表中"

**原因**：小程序后台没有配置后端域名，或域名不对。

**解决**：
1. 确认小程序后台已配置 `https://two-track-notebook-production.up.railway.app`
2. 在开发者工具中，打开「详情」→「本地设置」→ 勾选「不校验合法域名...」（仅开发测试用）
3. 重新编译

### Q2: Railway 域名在中国大陆无法访问

**原因**：`.railway.app` 域名可能被中国大陆网络限制。

**解决方案**：
- **方案A**：购买国内云服务器（阿里云/腾讯云）部署后端，使用备案域名
- **方案B**：使用 NATAPP 内网穿透 + 已备案域名（复杂）
- **方案C**：使用 Vercel 部署前端 + Railway 后端，H5 版本走浏览器访问（不受小程序限制）

### Q3: 编译报错 "找不到模块"

**原因**：依赖未安装或 node_modules 损坏。

**解决**：
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build:mp-weixin
```

### Q4: 页面显示空白或样式错乱

**原因**：部分 CSS 特性在小程序中支持不完整。

**解决**：
1. 检查是否使用了 `vh`、`vw` 等小程序不完全支持的单位（已检查，你的代码使用 `rpx`，兼容）
2. 检查是否有 `position: fixed` 在 scroll-view 内部的问题
3. 在微信开发者工具中查看 Wxml 和 Console 面板调试

### Q5: 登录后跳转失败，提示 "找不到页面"

**原因**：小程序的 `uni.reLaunch` 和 `uni.switchTab` 用法与 H5 略有差异。

**解决**：检查 `pages.json` 中 tabBar 页面路径是否正确，确保 `uni.switchTab` 的目标页面在 `tabBar.list` 中。

## 七、技术备注

### 已完成的适配工作

| 适配项 | 状态 | 说明 |
|--------|------|------|
| `pages.json` 路由配置 | ✅ | 已创建，包含所有页面和 tabBar |
| `manifest.json` 小程序配置 | ✅ | 已创建，mp-weixin 配置就绪 |
| `main.ts` 去掉 SSR | ✅ | 已改为 `createApp`，兼容小程序 |
| `request.ts` 条件编译 | ✅ | H5 走 Vite 代理，小程序走完整 URL |
| 代码中无 `window`/`document` | ✅ | 全部使用 `uni.` API，天然兼容 |
| 样式使用 `rpx` | ✅ | 适配小程序尺寸单位 |
| 组件使用 Uni-app 组件 | ✅ | `scroll-view`、`picker` 等兼容 |

### 后端改动需求

**无需任何改动！** 你的后端是 REST API + JWT 架构，天然支持多端。只需要在小程序后台配置合法域名即可。

### 如需添加微信登录（可选）

当前小程序使用用户名/密码登录，与 H5 一致。如果想支持微信一键登录，需要后端新增接口（如 `/api/auth/wx-login`），但不是必须的。

## 八、快速检查清单

- [ ] 注册小程序账号并获取 AppID
- [ ] 在 `manifest.json` 中填写 AppID
- [ ] 重新编译 `npm run build:mp-weixin`
- [ ] 小程序后台配置 `request 合法域名`
- [ ] 下载微信开发者工具并导入项目
- [ ] 测试登录、任务列表、探索轨等核心功能
- [ ] 上传代码并提交审核
