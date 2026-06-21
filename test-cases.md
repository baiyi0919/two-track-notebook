# 双轨笔记本 — 后端接口测试用例

> 项目：two-track-notebook  
> 技术栈：Spring Boot 3.5 + MyBatis-Plus 3.5.9 + Sa-Token JWT  
> 测试日期：2026-06-18  
> 接口前缀：/api（所有接口均含此前缀，如 /api/auth/login）

---

## 一、测试用例概览

### 1.1 接口清单

| 模块 | Controller | 接口数 | 需登录 | 备注 |
|------|------------|--------|--------|------|
| 认证 | AuthController | 2 | 否 | 注册/登录，无@SaCheckLogin |
| 用户 | UserController | 2 | 是 | 个人信息 |
| 现实轨 | TaskController | 6 | 是 | 任务CRUD + 状态切换 |
| 探索轨 | ThreadController | 7 | 是 | 议题CRUD + 消息 + 灵感 |
| 原则库 | PrincipleController | 6 | 是 | 原则CRUD + 反向关联 |
| 注意力审计 | AttentionLogController | 4 | 是 | 日志记录 + 日期查询 |
| 注意力报告 | ReportController | 2 | 是 | 日报 + 趋势 |
| AI对话 | AiController | 1 | 否 | 无@SaCheckLogin |
| 角色配置 | PersonaConfigController | 5 | 是 | 角色CRUD |
| 议题角色 | ThreadPersonaController | 3 | 是 | 议题内角色增删查 |
| 知识引用 | ReferenceController | 4 | 是 | 引用关系 |
| 分析框架 | AnalysisFrameworkController | 7 | 是 | 框架管理 + 定时更新 |
| **合计** | **12** | **49** | — | — |

### 1.2 统一响应格式

```json
{
  "code": 200,      // 200=成功，500=失败，401=未登录
  "message": "success",
  "data": { ... }
}
```

### 1.3 认证方式

所有需登录接口在请求头携带：
```
Authorization: Bearer <JWT Token>
```
Token 通过 `/api/auth/login` 或 `/api/auth/register` 获取。

---

## 二、认证模块（AuthController）

### 2.1 注册 — POST /api/auth/register

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AUTH-01 | 正常注册 | `{"username":"testuser","password":"123456","nickname":"测试用户"}` | code=200, data=JWT Token | P0 |
| AUTH-02 | 用户名过短 | `{"username":"ab","password":"123456"}` | code=500, message=用户名长度3-50位 | P1 |
| AUTH-03 | 用户名超长 | `{"username":"a" * 51,"password":"123456"}` | code=500, message=用户名长度3-50位 | P1 |
| AUTH-04 | 用户名空 | `{"username":"","password":"123456"}` | code=500, message=用户名不能为空 | P1 |
| AUTH-05 | 密码过短 | `{"username":"testuser","password":"12345"}` | code=500, message=密码长度6-20位 | P1 |
| AUTH-06 | 密码超长 | `{"username":"testuser","password":"1" * 21}` | code=500, message=密码长度6-20位 | P1 |
| AUTH-07 | 密码空 | `{"username":"testuser","password":""}` | code=500, message=密码不能为空 | P1 |
| AUTH-08 | 用户名已存在 | `{"username":"admin","password":"123456"}` | code=500, message=用户名已存在 | P1 |
| AUTH-09 | 无昵称注册 | `{"username":"nonick","password":"123456"}` | code=200, data=JWT Token, nickname=null | P1 |
| AUTH-10 | 中文用户名 | `{"username":"测试用户","password":"123456"}` | code=200 | P2 |
| AUTH-11 | SQL注入尝试 | `{"username":"'; DROP TABLE user; --","password":"123456"}` | code=500（参数校验拦截） | P2 |

### 2.2 登录 — POST /api/auth/login

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AUTH-12 | 正常登录 | `{"username":"testuser","password":"123456"}` | code=200, data=JWT Token | P0 |
| AUTH-13 | 用户名不存在 | `{"username":"notexist","password":"123456"}` | code=500, message=用户不存在 | P1 |
| AUTH-14 | 密码错误 | `{"username":"testuser","password":"wrongpass"}` | code=500, message=密码错误 | P1 |
| AUTH-15 | 空用户名 | `{"username":"","password":"123456"}` | code=500, message=用户名不能为空 | P1 |
| AUTH-16 | 空密码 | `{"username":"testuser","password":""}` | code=500, message=密码不能为空 | P1 |
| AUTH-17 | 已删除用户登录 | 使用 isDeleted=1 的用户登录 | code=500, message=用户不存在 | P2 |
| AUTH-18 | Token有效期 | 登录成功，等待30天+再请求 | code=401, message=Token已过期 | P2 |
| AUTH-19 | 并发登录 | 同一账号在多处登录 | 都成功（is-concurrent=true） | P2 |

---

## 三、用户模块（UserController）

### 3.1 获取当前用户信息 — GET /api/user/info

| 用例编号 | 场景 | 请求头 | 预期结果 | 优先级 |
|----------|------|--------|----------|--------|
| USER-01 | 正常获取 | 有效Token | code=200, data={id,username,nickname,avatarUrl} | P0 |
| USER-02 | 未登录 | 无Token | code=401 | P1 |
| USER-03 | Token过期 | 过期Token | code=401 | P1 |
| USER-04 | Token无效 | 伪造Token | code=401 | P1 |
| USER-05 | 已删除用户 | 被删除用户的Token | code=401 | P2 |

### 3.2 更新用户信息 — PUT /api/user/info

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| USER-06 | 更新昵称 | `{"nickname":"新昵称"}` | code=200, nickname=新昵称 | P0 |
| USER-07 | 更新头像 | `{"avatarUrl":"https://..."}` | code=200 | P1 |
| USER-08 | 空昵称 | `{"nickname":""}` | code=500 或 code=200(允许) | P1 |
| USER-09 | 未登录 | 无Token | code=401 | P1 |
| USER-10 | 超长昵称 | 长度>100 | 视校验规则 | P2 |

---

## 四、现实轨 — 任务模块（TaskController）

### 4.1 创建任务 — POST /api/tasks

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TASK-01 | 正常创建 | `{"title":"学习Spring","anchorText":"为了提升后端能力","budget":2.5}` | code=200, id自增, userId=当前用户 | P0 |
| TASK-02 | 标题为空 | `{"title":""}` | code=500, message=任务标题不能为空 | P1 |
| TASK-03 | 标题超100字 | title长度101 | code=500, message=任务标题不超过100字 | P1 |
| TASK-04 | 锚点超500字 | anchorText长度501 | code=500, message=现实锚点不超过500字 | P1 |
| TASK-05 | budget为负数 | `{"budget":-1}` | code=500 或 后端校验 | P1 |
| TASK-06 | 未登录 | 无Token | code=401 | P1 |
| TASK-07 | 关联原则 | `{"title":"xxx","principleId":1}` | code=200, principleId=1 | P1 |
| TASK-08 | 关联不存在原则 | `{"principleId":99999}` | code=500（外键约束） | P1 |
| TASK-09 | 只有必填项 | `{"title":"最小任务"}` | code=200, 其他字段null | P2 |
| TASK-10 | dueDate为过去日期 | `{"dueDate":"2020-01-01"}` | code=200（允许过去日期） | P2 |
| TASK-11 | 超长期预算 | `{"budget":10000}` | code=200（无上限校验） | P2 |
| TASK-12 | XSS注入 | `{"title":"<script>alert(1)</script>"}` | code=200（XSS由前端过滤或后端转义） | P2 |

### 4.2 查询任务列表 — GET /api/tasks

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TASK-13 | 无筛选 | — | 返回当前用户所有任务 | P0 |
| TASK-14 | 按状态筛选 | `?status=0` | 只返回待完成任务 | P1 |
| TASK-15 | 按状态筛选 | `?status=1` | 只返回已完成任务 | P1 |
| TASK-16 | 按状态筛选 | `?status=2` | 只返回已放弃任务 | P1 |
| TASK-17 | 按状态筛选 | `?status=999` | 空列表或 code=200 | P1 |
| TASK-18 | 按创建时间排序 | `?sortBy=createdAt&sortOrder=desc` | 按时间倒序 | P1 |
| TASK-19 | 按标题排序 | `?sortBy=title&sortOrder=asc` | 按标题正序 | P1 |
| TASK-20 | 未登录 | — | code=401 | P1 |
| TASK-21 | 无数据 | — | code=200, data=[] | P1 |
| TASK-22 | 其他用户数据隔离 | 用户A登录 | 只返回用户A的任务，不返回用户B | P1 |
| TASK-23 | 已删除任务不显示 | 用户删除过任务 | 不返回 isDeleted=1 的任务 | P1 |

### 4.3 查询任务详情 — GET /api/tasks/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TASK-24 | 正常查询 | `/tasks/1` | code=200, 完整Task对象 | P0 |
| TASK-25 | 不存在的ID | `/tasks/99999` | code=200, data=null 或 code=500 | P1 |
| TASK-26 | 其他用户的任务 | `/tasks/2` (用户B的任务) | code=500, message=无权访问 | P1 |
| TASK-27 | 已删除的任务 | `/tasks/3` (已删除) | code=500 或 data=null | P1 |
| TASK-28 | 非法ID | `/tasks/abc` | 400 Bad Request | P1 |
| TASK-29 | 未登录 | — | code=401 | P1 |
| TASK-30 | ID为0 | `/tasks/0` | code=500 | P2 |
| TASK-31 | ID为负数 | `/tasks/-1` | code=500 | P2 |

### 4.4 更新任务 — PUT /api/tasks/{id}

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TASK-32 | 正常更新标题 | `{"title":"新标题"}` | code=200, title=新标题 | P0 |
| TASK-33 | 更新实际用时 | `{"actualTime":3.5}` | code=200, actualTime=3.5 | P1 |
| TASK-34 | 更新关联原则 | `{"principleId":0}` | code=200, principleId=null (清除关联) | P1 |
| TASK-35 | 更新关联原则 | `{"principleId":2}` | code=200, principleId=2 | P1 |
| TASK-36 | 更新状态 | `{"status":1}` | code=200, status=1 | P1 |
| TASK-37 | 更新不存在的任务 | `/tasks/99999` | code=500 | P1 |
| TASK-38 | 更新其他用户的任务 | `/tasks/2` | code=500, message=无权访问 | P1 |
| TASK-39 | 全部字段空 | `{}` | code=200（不修改）或 200 | P2 |
| TASK-40 | 未登录 | 无Token | code=401 | P1 |
| TASK-41 | 并发更新 | 两处同时修改 | 后提交的覆盖（无乐观锁） | P2 |

### 4.5 删除任务 — DELETE /api/tasks/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TASK-42 | 正常删除 | `/tasks/1` | code=200, 软删除（isDeleted=1） | P0 |
| TASK-43 | 删除不存在的任务 | `/tasks/99999` | code=500 | P1 |
| TASK-44 | 删除其他用户的任务 | `/tasks/2` | code=500 | P1 |
| TASK-45 | 重复删除 | 再次删除已删除的 | code=500 | P1 |
| TASK-46 | 删除后查询 | 删除后再 GET /tasks/1 | code=500 或 data=null | P1 |
| TASK-47 | 未登录 | 无Token | code=401 | P1 |

### 4.6 切换任务状态 — PATCH /api/tasks/{id}/status

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TASK-48 | 标记完成 | `?status=1` | code=200, status=1 | P0 |
| TASK-49 | 标记放弃 | `?status=2` | code=200, status=2 | P1 |
| TASK-50 | 重新打开 | `?status=0` | code=200, status=0 | P1 |
| TASK-51 | 非法状态 | `?status=999` | code=500 或 不报错但无意义 | P1 |
| TASK-52 | 未登录 | 无Token | code=401 | P1 |
| TASK-53 | 其他用户的任务 | `/tasks/2` | code=500 | P1 |

---

## 五、探索轨 — 议题模块（ThreadController）

### 5.1 创建议题 — POST /api/threads

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| THREAD-01 | 正常创建 | `{"topic":"如何学习Spring","description":"想系统学习后端"}` | code=200, id自增 | P0 |
| THREAD-02 | 标题为空 | `{"topic":""}` | code=500, message=议题标题不能为空 | P1 |
| THREAD-03 | 标题超200字 | topic长度201 | code=500, message=议题标题不超过200字 | P1 |
| THREAD-04 | 描述超1000字 | description长度1001 | code=500, message=议题描述不超过1000字 | P1 |
| THREAD-05 | 无描述 | `{"topic":"只有标题"}` | code=200, description=null | P1 |
| THREAD-06 | 未登录 | 无Token | code=401 | P1 |

### 5.2 查询议题列表 — GET /api/threads

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| THREAD-07 | 无筛选 | — | 返回当前用户所有议题 | P0 |
| THREAD-08 | 按状态筛选 | `?status=0` | 只返回开放议题 | P1 |
| THREAD-09 | 按状态筛选 | `?status=1` | 只返回已总结议题 | P1 |
| THREAD-10 | 未登录 | 无Token | code=401 | P1 |
| THREAD-11 | 数据隔离 | 用户A登录 | 只返回用户A的议题 | P1 |
| THREAD-12 | 已删除不显示 | — | 不返回 isDeleted=1 的议题 | P1 |

### 5.3 查询议题详情 — GET /api/threads/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| THREAD-13 | 正常查询 | `/threads/1` | code=200 | P0 |
| THREAD-14 | 不存在 | `/threads/99999` | code=200,data=null 或 500 | P1 |
| THREAD-15 | 其他用户的 | `/threads/2` | code=500 | P1 |
| THREAD-16 | 已删除的 | `/threads/3` | code=500 | P1 |
| THREAD-17 | 未登录 | 无Token | code=401 | P1 |

### 5.4 更新议题 — PUT /api/threads/{id}

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| THREAD-18 | 更新标题 | `{"topic":"新标题"}` | code=200 | P0 |
| THREAD-19 | 更新描述 | `{"description":"新描述"}` | code=200 | P1 |
| THREAD-20 | 更新不存在的 | `/threads/99999` | code=500 | P1 |
| THREAD-21 | 更新其他用户的 | `/threads/2` | code=500 | P1 |
| THREAD-22 | 未登录 | 无Token | code=401 | P1 |

### 5.5 删除议题 — DELETE /api/threads/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| THREAD-23 | 正常删除 | `/threads/1` | code=200, 软删除 | P0 |
| THREAD-24 | 删除不存在的 | `/threads/99999` | code=500 | P1 |
| THREAD-25 | 删除其他用户的 | `/threads/2` | code=500 | P1 |
| THREAD-26 | 删除后关联消息 | 删除后 GET /threads/1/messages | code=200,data=[] | P1 |
| THREAD-27 | 未登录 | 无Token | code=401 | P1 |

### 5.6 关闭议题 — PATCH /api/threads/{id}/close

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| THREAD-28 | 正常关闭 | `/threads/1/close` | code=200, status=1 | P0 |
| THREAD-29 | 关闭已关闭的 | 再次关闭 | code=200 或 500 | P1 |
| THREAD-30 | 关闭其他用户的 | `/threads/2/close` | code=500 | P1 |
| THREAD-31 | 未登录 | 无Token | code=401 | P1 |

### 5.7 议题下发言 — POST /api/threads/{id}/messages

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| MSG-01 | 正常发言 | `{"content":"我觉得可以这样"}` | code=200, id自增, userId=当前用户, personaId=null | P0 |
| MSG-02 | 内容为空 | `{"content":""}` | code=500, message=消息内容不能为空 | P1 |
| MSG-03 | 内容超5000字 | content长度5001 | code=500, message=消息内容不超过5000字 | P1 |
| MSG-04 | 以角色发言 | `{"content":"xxx","personaId":1}` | code=200, personaId=1 | P1 |
| MSG-05 | personaId=0 | `{"content":"xxx","personaId":0}` | **code=500（外键约束）** | P1 |
| MSG-06 | personaId=null | `{"content":"xxx","personaId":null}` | code=200, personaId=null | P1 |
| MSG-07 | 不存在的personaId | `{"personaId":99999}` | code=500（外键约束） | P1 |
| MSG-08 | 不存在的threadId | `/threads/99999/messages` | code=500 | P1 |
| MSG-09 | 其他用户的thread | `/threads/2/messages` | code=500 | P1 |
| MSG-10 | 未登录 | 无Token | code=401 | P1 |
| MSG-11 | 含引用灵感 | `{"content":"xxx","refInspirationId":1}` | code=200 | P2 |
| MSG-12 | 含角色名 | `{"content":"xxx","roleName":"专家"}` | code=200 | P2 |

### 5.8 查询议题消息列表 — GET /api/threads/{id}/messages

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| MSG-13 | 正常查询 | `/threads/1/messages` | code=200, 按时间正序 | P0 |
| MSG-14 | 无消息 | `/threads/1/messages` | code=200, data=[] | P1 |
| MSG-15 | 不存在的thread | `/threads/99999/messages` | code=500 | P1 |
| MSG-16 | 其他用户的thread | `/threads/2/messages` | code=500 | P1 |
| MSG-17 | 未登录 | 无Token | code=401 | P1 |
| MSG-18 | 删除议题后 | 删除后再查 | code=200, data=[] | P1 |

### 5.9 灵感快寄 — POST /api/threads/inspiration

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| INS-01 | 正常保存 | `{"content":"突然想到一个点子"}` | code=200, 自动创建议题并关闭 | P0 |
| INS-02 | 内容为空 | `{"content":""}` | code=500 | P1 |
| INS-03 | 超长内容 | 长度>50的标题截取 | 议题标题=💡 灵感: + 前50字 | P2 |
| INS-04 | 未登录 | 无Token | code=401 | P1 |

---

## 六、原则库模块（PrincipleController）

### 6.1 创建原则 — POST /api/principles

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PRIN-01 | 正常创建 | `{"content":"每天写代码至少1小时"}` | code=200, id自增 | P0 |
| PRIN-02 | 内容为空 | `{"content":""}` | code=500, message=原则内容不能为空 | P1 |
| PRIN-03 | 内容超500字 | content长度501 | code=500, message=原则内容不超过500字 | P1 |
| PRIN-04 | 带来源议题 | `{"content":"xxx","sourceThreadId":1}` | code=200 | P1 |
| PRIN-05 | 带标签 | `{"content":"xxx","tags":"学习,编程"}` | code=200 | P1 |
| PRIN-06 | 未登录 | 无Token | code=401 | P1 |

### 6.2 查询原则列表 — GET /api/principles

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PRIN-07 | 无筛选 | — | 返回当前用户所有原则 | P0 |
| PRIN-08 | 关键词检索 | `?keyword=学习` | 返回含"学习"的原则 | P1 |
| PRIN-09 | 关键词无匹配 | `?keyword=xyznotexist` | code=200, data=[] | P1 |
| PRIN-10 | 未登录 | 无Token | code=401 | P1 |
| PRIN-11 | 数据隔离 | 用户A登录 | 只返回用户A的原则 | P1 |

### 6.3 查询原则详情 — GET /api/principles/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PRIN-12 | 正常查询 | `/principles/1` | code=200 | P0 |
| PRIN-13 | 不存在 | `/principles/99999` | code=200,data=null 或 500 | P1 |
| PRIN-14 | 其他用户的 | `/principles/2` | code=500 | P1 |
| PRIN-15 | 未登录 | 无Token | code=401 | P1 |

### 6.4 更新原则 — PUT /api/principles/{id}

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PRIN-16 | 更新内容 | `{"content":"修改后的原则"}` | code=200 | P0 |
| PRIN-17 | 更新标签 | `{"tags":"新标签"}` | code=200 | P1 |
| PRIN-18 | 更新不存在的 | `/principles/99999` | code=500 | P1 |
| PRIN-19 | 更新其他用户的 | `/principles/2` | code=500 | P1 |
| PRIN-20 | 未登录 | 无Token | code=401 | P1 |

### 6.5 删除原则 — DELETE /api/principles/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PRIN-21 | 正常删除 | `/principles/1` | code=200, 软删除 | P0 |
| PRIN-22 | 删除已关联任务的 | `/principles/1` (被任务关联) | 原则删除，任务 principleId 变成 null | P1 |
| PRIN-23 | 删除不存在的 | `/principles/99999` | code=500 | P1 |
| PRIN-24 | 删除其他用户的 | `/principles/2` | code=500 | P1 |
| PRIN-25 | 未登录 | 无Token | code=401 | P1 |

### 6.6 查询关联任务 — GET /api/principles/{id}/related-tasks

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PRIN-26 | 有关联任务 | `/principles/1/related-tasks` | code=200, 返回关联的任务列表 | P0 |
| PRIN-27 | 无关联任务 | `/principles/2/related-tasks` | code=200, data=[] | P1 |
| PRIN-28 | 不存在的原则 | `/principles/99999/related-tasks` | code=500 | P1 |
| PRIN-29 | 其他用户的原则 | `/principles/2/related-tasks` | code=500 | P1 |
| PRIN-30 | 未登录 | 无Token | code=401 | P1 |

---

## 七、注意力审计模块（AttentionLogController）

### 7.1 写入/更新日志 — POST /api/attention-logs

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| LOG-01 | 正常创建 | `{"taskId":1,"logDate":"2026-06-18","budget":2,"actualTime":1.5}` | code=200, id自增 | P0 |
| LOG-02 | 同日期同任务更新 | 再次POST相同taskId和logDate | code=200, 更新原有记录（覆盖或累加） | P1 |
| LOG-03 | 无taskId | `{"logDate":"2026-06-18","budget":2}` | code=200（taskId可为null） | P1 |
| LOG-04 | budget为负数 | `{"budget":-1}` | code=500 或 允许 | P1 |
| LOG-05 | actualTime> budget | `{"budget":2,"actualTime":5}` | code=200（超预算不报错） | P1 |
| LOG-06 | 未登录 | 无Token | code=401 | P1 |
| LOG-07 | 超长日期范围 | logDate=未来10年 | code=200 | P2 |

### 7.2 按日期查询 — GET /api/attention-logs/by-date

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| LOG-08 | 正常查询 | `?date=2026-06-18` | code=200, 返回该日所有日志 | P0 |
| LOG-09 | 无数据 | `?date=2020-01-01` | code=200, data=[] | P1 |
| LOG-10 | 日期格式错误 | `?date=2026/06/18` | code=400 Bad Request | P1 |
| LOG-11 | 未登录 | 无Token | code=401 | P1 |
| LOG-12 | 数据隔离 | 用户A查询 | 只返回用户A的日志 | P1 |

### 7.3 按任务查询 — GET /api/attention-logs/by-task/{taskId}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| LOG-13 | 正常查询 | `/by-task/1` | code=200, 返回该任务所有日志 | P0 |
| LOG-14 | 无数据 | `/by-task/99999` | code=200, data=[] | P1 |
| LOG-15 | 其他用户的任务 | `/by-task/2` (用户B的任务) | code=500 或 空列表 | P1 |
| LOG-16 | 未登录 | 无Token | code=401 | P1 |

### 7.4 按日期范围查询 — GET /api/attention-logs/by-range

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| LOG-17 | 正常查询 | `?start=2026-06-01&end=2026-06-18` | code=200, 返回范围内日志 | P0 |
| LOG-18 | start > end | `?start=2026-06-18&end=2026-06-01` | code=200, data=[] 或 500 | P1 |
| LOG-19 | 缺少参数 | 只有start | code=400 或 500 | P1 |
| LOG-20 | 超长范围 | `?start=2020-01-01&end=2030-01-01` | code=200 | P2 |
| LOG-21 | 未登录 | 无Token | code=401 | P1 |

---

## 八、注意力报告模块（ReportController）

### 8.1 注意力审计报告 — GET /api/reports/attention

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| REPORT-01 | 指定日期 | `?date=2026-06-18` | code=200, 返回当日报告 | P0 |
| REPORT-02 | 默认日期 | — | code=200, 返回今天报告 | P1 |
| REPORT-03 | 日期格式错误 | `?date=06-18-2026` | code=400 | P1 |
| REPORT-04 | 无数据 | `?date=2020-01-01` | code=200, 空报告 | P1 |
| REPORT-05 | 未登录 | 无Token | code=401 | P1 |

### 8.2 注意力趋势 — GET /api/reports/attention/trend

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| REPORT-06 | 7天趋势 | `?days=7` (默认) | code=200, 最近7天趋势 | P0 |
| REPORT-07 | 30天趋势 | `?days=30` | code=200, 最近30天趋势 | P1 |
| REPORT-08 | 1天趋势 | `?days=1` | code=200, 最近1天 | P1 |
| REPORT-09 | 超大天数 | `?days=365` | code=200 | P2 |
| REPORT-10 | 负数天数 | `?days=-1` | code=500 或 400 | P1 |
| REPORT-11 | 未登录 | 无Token | code=401 | P1 |

---

## 九、AI对话模块（AiController）

### 9.1 触发AI回复 — POST /api/ai/chat

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AI-01 | 正常触发 | `{"threadId":1,"personaId":1}` | code=200, 返回AI生成的Message | P0 |
| AI-02 | 不存在的threadId | `{"threadId":99999,"personaId":1}` | code=500 | P1 |
| AI-03 | 不存在的personaId | `{"threadId":1,"personaId":99999}` | code=500 | P1 |
| AI-04 | threadId为空 | `{"personaId":1}` | code=500, message=议题ID不能为空 | P1 |
| AI-05 | personaId为空 | `{"threadId":1}` | code=500, message=角色ID不能为空 | P1 |
| AI-06 | 其他用户的thread | `{"threadId":2,"personaId":1}` | code=500 | P1 |
| AI-07 | 未登录 | 无Token | code=200 (无@SaCheckLogin) | P1 |
| AI-08 | AI服务超时 | 网络问题 | code=500, message=AI服务异常 | P2 |
| AI-09 | AI服务返回空 | 特殊场景 | code=200, data=null 或 500 | P2 |

---

## 十、角色配置模块（PersonaConfigController）

### 10.1 创建角色 — POST /api/persona-configs

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PC-01 | 正常创建 | `{"name":"专家","prompt":"你是一个专家..."}` | code=200, id自增 | P0 |
| PC-02 | 空名称 | `{"name":""}` | 视校验规则 | P1 |
| PC-03 | 未登录 | 无Token | code=401 | P1 |
| PC-04 | 超长prompt | 长度>10000 | code=200 或 500 | P2 |

### 10.2 查询我的角色 — GET /api/persona-configs/mine

| 用例编号 | 场景 | 预期结果 | 优先级 |
|----------|------|----------|--------|
| PC-05 | 正常查询 | code=200, 返回当前用户所有角色 | P0 |
| PC-06 | 无角色 | code=200, data=[] | P1 |
| PC-07 | 未登录 | code=401 | P1 |
| PC-08 | 数据隔离 | 只返回当前用户的角色 | P1 |

### 10.3 查询角色详情 — GET /api/persona-configs/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PC-09 | 正常查询 | `/persona-configs/1` | code=200 | P0 |
| PC-10 | 不存在 | `/persona-configs/99999` | code=200,data=null 或 500 | P1 |
| PC-11 | 其他用户的 | `/persona-configs/2` | code=500 | P1 |
| PC-12 | 未登录 | 无Token | code=401 | P1 |

### 10.4 更新角色 — PUT /api/persona-configs/{id}

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PC-13 | 更新prompt | `{"prompt":"新提示词"}` | code=200 | P0 |
| PC-14 | 更新名称 | `{"name":"新名称"}` | code=200 | P1 |
| PC-15 | 更新不存在的 | `/persona-configs/99999` | code=500 | P1 |
| PC-16 | 更新其他用户的 | `/persona-configs/2` | code=500 | P1 |
| PC-17 | 未登录 | 无Token | code=401 | P1 |

### 10.5 删除角色 — DELETE /api/persona-configs/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| PC-18 | 正常删除 | `/persona-configs/1` | code=200, 软删除 | P0 |
| PC-19 | 删除已被消息引用的 | 某message用了personaId=1 | 角色删除，message保留但personaId指向null | P1 |
| PC-20 | 删除不存在的 | `/persona-configs/99999` | code=500 | P1 |
| PC-21 | 删除其他用户的 | `/persona-configs/2` | code=500 | P1 |
| PC-22 | 未登录 | 无Token | code=401 | P1 |

---

## 十一、议题角色模块（ThreadPersonaController）

### 11.1 查询议题角色 — GET /api/thread-personas

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TP-01 | 正常查询 | `?threadId=1` | code=200, 返回该议题激活的角色列表 | P0 |
| TP-02 | 无角色 | `?threadId=1` | code=200, data=[] | P1 |
| TP-03 | threadId为空 | — | code=400 或 500 | P1 |
| TP-04 | 其他用户的thread | `?threadId=2` | code=500 | P1 |
| TP-05 | 未登录 | 无Token | code=401 | P1 |

### 11.2 添加角色到议题 — POST /api/thread-personas

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TP-06 | 正常添加 | `?threadId=1&personaId=2` | code=200, 角色加入议题 | P0 |
| TP-07 | 重复添加 | 再次添加相同 | code=200 或 500（已存在） | P1 |
| TP-08 | 添加已隐藏的角色 | 之前隐藏过该角色 | code=200, 恢复显示 | P1 |
| TP-09 | 不存在的personaId | `?threadId=1&personaId=99999` | code=500 | P1 |
| TP-10 | 其他用户的thread | `?threadId=2&personaId=1` | code=500 | P1 |
| TP-11 | 未登录 | 无Token | code=401 | P1 |

### 11.3 从议题隐藏角色 — DELETE /api/thread-personas

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| TP-12 | 正常隐藏 | `?threadId=1&personaId=2` | code=200, 软删除 | P0 |
| TP-13 | 隐藏不存在的 | `?threadId=1&personaId=99999` | code=500 | P1 |
| TP-14 | 其他用户的thread | `?threadId=2&personaId=1` | code=500 | P1 |
| TP-15 | 未登录 | 无Token | code=401 | P1 |

---

## 十二、知识引用模块（ReferenceController）

### 12.1 创建引用 — POST /api/references

| 用例编号 | 场景 | 输入 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| REF-01 | 正常创建 | `{"sourceType":"MESSAGE","sourceId":1,"targetType":"TASK","targetId":1}` | code=200, id自增 | P0 |
| REF-02 | 空sourceType | `{"sourceType":""}` | code=500, message=来源类型不能为空 | P1 |
| REF-03 | 空sourceId | `{"sourceId":null}` | code=500, message=来源ID不能为空 | P1 |
| REF-04 | 自引用 | `{"sourceType":"TASK","sourceId":1,"targetType":"TASK","targetId":1}` | code=200 或 500（是否允许自引用） | P2 |
| REF-05 | 循环引用 | A→B, B→A | code=200（无循环检测） | P2 |
| REF-06 | 未登录 | 无Token | code=401 | P1 |

### 12.2 删除引用 — DELETE /api/references/{id}

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| REF-07 | 正常删除 | `/references/1` | code=200, 物理删除 | P0 |
| REF-08 | 删除不存在的 | `/references/99999` | code=500 | P1 |
| REF-09 | 删除其他用户的引用 | `/references/2` | code=500 | P1 |
| REF-10 | 未登录 | 无Token | code=401 | P1 |

### 12.3 按来源查询 — GET /api/references/source

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| REF-11 | 正常查询 | `?sourceType=MESSAGE&sourceId=1` | code=200, 返回该实体的引用 | P0 |
| REF-12 | 无引用 | `?sourceType=MESSAGE&sourceId=99999` | code=200, data=[] | P1 |
| REF-13 | 参数缺失 | 缺少sourceId | code=400 或 500 | P1 |
| REF-14 | 未登录 | 无Token | code=401 | P1 |

### 12.4 按目标查询（反向链接）— GET /api/references/target

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| REF-15 | 正常查询 | `?targetType=TASK&targetId=1` | code=200, 返回引用该目标的实体 | P0 |
| REF-16 | 无反向链接 | `?targetType=TASK&targetId=99999` | code=200, data=[] | P1 |
| REF-17 | 未登录 | 无Token | code=401 | P1 |

---

## 十三、分析框架模块（AnalysisFrameworkController）

### 13.1 保存/更新框架 — POST /api/analysis-frameworks

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AF-01 | 正常保存 | `?personaId=1` + body="框架内容" | code=200, 保存或更新 | P0 |
| AF-02 | 不存在的personaId | `?personaId=99999` | code=500 | P1 |
| AF-03 | 空内容 | body="" | code=200 或 500 | P1 |
| AF-04 | 未登录 | 无Token | code=401 | P1 |

### 13.2 查询框架 — GET /api/analysis-frameworks

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AF-05 | 正常查询 | `?personaId=1` | code=200, 返回框架内容 | P0 |
| AF-06 | 无框架 | `?personaId=1` | code=200, data=null | P1 |
| AF-07 | 未登录 | 无Token | code=401 | P1 |

### 13.3 更新下次更新时间 — PUT /api/analysis-frameworks/next-update-time

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AF-08 | 正常更新 | `?personaId=1&nextTime=2026-06-20T10:00:00` | code=200 | P1 |
| AF-09 | 时间格式错误 | `?nextTime=2026-06-20` | code=400 或 500 | P1 |
| AF-10 | 未登录 | 无Token | code=401 | P1 |

### 13.4 查询待更新数量 — GET /api/analysis-frameworks/pending-count

| 用例编号 | 场景 | 预期结果 | 优先级 |
|----------|------|----------|--------|
| AF-11 | 有1个待更新 | code=200, data=1 | P1 |
| AF-12 | 无待更新 | code=200, data=0 | P1 |
| AF-13 | 未登录 | code=401 | P1 |

### 13.5 查询待更新列表 — GET /api/analysis-frameworks/pending-list

| 用例编号 | 场景 | 预期结果 | 优先级 |
|----------|------|----------|--------|
| AF-14 | 有列表 | code=200, 返回列表 | P1 |
| AF-15 | 无列表 | code=200, data=[] | P1 |
| AF-16 | 未登录 | code=401 | P1 |

### 13.6 触发更新 — POST /api/analysis-frameworks/trigger-update

| 用例编号 | 场景 | 参数 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| AF-17 | 正常触发 | `?personaId=1` | code=200, 框架内容已更新 | P1 |
| AF-18 | 不存在的personaId | `?personaId=99999` | code=500 | P1 |
| AF-19 | 未登录 | 无Token | code=401 | P1 |
| AF-20 | AI超时 | 网络问题 | code=500 | P2 |

### 13.7 批量补全 — POST /api/analysis-frameworks/ensure-all

| 用例编号 | 场景 | 预期结果 | 优先级 |
|----------|------|----------|--------|
| AF-21 | 正常补全 | code=200, data=补全数量 | P1 |
| AF-22 | 已全存在 | code=200, data=0 | P1 |
| AF-23 | 未登录 | code=401 | P1 |

---

## 十四、跨模块集成测试

### 14.1 完整业务流测试

| 用例编号 | 场景 | 操作步骤 | 预期结果 | 优先级 |
|----------|------|----------|----------|--------|
| INTEG-01 | 用户注册→登录→创建任务 | 1. POST /auth/register 2. POST /auth/login 3. POST /tasks | 每步code=200, 最终创建成功 | P0 |
| INTEG-02 | 创建议题→发言→关闭→提取原则 | 1. POST /threads 2. POST /threads/{id}/messages 3. PATCH /threads/{id}/close 4. POST /principles | 每步code=200, 原则sourceThreadId正确 | P0 |
| INTEG-03 | 创建任务→关联原则→记录注意力→查看报告 | 1. POST /tasks (含principleId) 2. POST /attention-logs 3. GET /reports/attention | 每步code=200, 报告含该日志 | P0 |
| INTEG-04 | 创建角色→配置框架→创建议题→AI对话 | 1. POST /persona-configs 2. POST /analysis-frameworks 3. POST /threads 4. POST /ai/chat | 每步code=200, AI返回Message | P1 |
| INTEG-05 | 消息引用任务→查看反向链接 | 1. POST /references 2. GET /references/target?targetType=TASK&targetId=1 | 每步code=200, 反向链接含该消息 | P1 |
| INTEG-06 | 删除任务后查询关联原则 | 1. DELETE /tasks/1 2. GET /principles/{id}/related-tasks | 原则反向关联列表中不再显示该任务 | P1 |
| INTEG-07 | 删除议题后查询原则 | 1. DELETE /threads/1 2. GET /principles/{id} (sourceThreadId=1) | 原则仍存在，但sourceThreadId指向已删除议题 | P1 |
| INTEG-08 | 删除用户后所有数据隔离 | 删除用户后，其他用户无法查询该用户数据 | 数据正确隔离 | P2 |

### 14.2 并发场景测试

| 用例编号 | 场景 | 操作 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| CONC-01 | 并发创建任务 | 两个请求同时 POST /tasks | 都成功，id不同，不冲突 | P2 |
| CONC-02 | 并发更新同一任务 | 两个请求同时 PUT /tasks/1 | 后提交的覆盖，无乐观锁报错 | P2 |
| CONC-03 | 并发删除同一任务 | 两个请求同时 DELETE /tasks/1 | 第一个成功，第二个失败（已删除） | P2 |
| CONC-04 | 并发发言 | 两个请求同时 POST /threads/1/messages | 都成功，时间戳不同 | P2 |

### 14.3 安全测试

| 用例编号 | 场景 | 操作 | 预期结果 | 优先级 |
|----------|------|------|----------|--------|
| SEC-01 | SQL注入 | `?keyword='; DROP TABLE user; --` | code=200 或 500，但数据库不被破坏 | P1 |
| SEC-02 | XSS注入 | `{"title":"<script>alert(1)</script>"}` | code=200，但前端渲染时安全过滤 | P1 |
| SEC-03 | 越权访问 | 用户A的Token访问用户B的数据 | code=500, message=无权访问 | P1 |
| SEC-04 | Token伪造 | 使用非法JWT | code=401 | P1 |
| SEC-05 | 重放攻击 | 使用相同Token多次请求 | 都成功（Token在有效期内） | P2 |
| SEC-06 | 暴力破解 | 连续错误登录 | 无限制（当前无登录频率限制） | P2 |
| SEC-07 | 大文件上传 | 上传超大图片 | 视文件大小限制 | P2 |

---

## 十五、测试环境配置

### 15.1 本地测试环境

```bash
# 1. 启动后端（默认端口8080）
cd backend
mvn spring-boot:run

# 2. 数据库初始化
mysql -u root -p123456 < sql/init.sql

# 3. 测试地址
Base URL: http://localhost:8080/api
```

### 15.2 测试数据准备

```sql
-- 测试用户
call init_test_data();  -- 如果有初始化存储过程
-- 或手动插入
INSERT INTO user (username, password, nickname) VALUES
('testuser', 'encrypted_pass', '测试用户'),
('userA', 'encrypted_pass', '用户A'),
('userB', 'encrypted_pass', '用户B');
```

### 15.3 测试工具推荐

| 工具 | 用途 | 推荐度 |
|------|------|--------|
| Postman | 手动接口测试 | ⭐⭐⭐⭐⭐ |
| Apifox | 国产替代，可生成文档 | ⭐⭐⭐⭐⭐ |
| JUnit | 后端单元测试 | ⭐⭐⭐⭐⭐ |
| curl | 命令行快速测试 | ⭐⭐⭐⭐ |
| 浏览器控制台 | 前端联调 | ⭐⭐⭐⭐ |

---

## 十六、测试用例执行记录表（空白模板）

| 用例编号 | 测试日期 | 测试人 | 环境 | 结果 | 实际输出 | 缺陷编号 | 备注 |
|----------|----------|--------|------|------|----------|----------|------|
| AUTH-01 | | | | | | | |
| AUTH-02 | | | | | | | |
| ... | | | | | | | |

---

*文档由 WorkBuddy AI 生成，最后更新：2026-06-18*
*总计：49个接口，约180个测试用例*
