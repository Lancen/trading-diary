# Auth API Contracts

**Feature**: 002-project-scaffold-user-auth | **Base**: `/api/v1/auth`

## POST /login

**Access**: 匿名

**Request**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Success Response** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "dGhpcyBp...",
    "expiresIn": 900
  },
  "timestamp": "2026-05-11T10:00:00+08:00"
}
```

**Error Responses**:
- 401 — 用户名或密码错误 `{code: 100101, message: "用户名或密码错误", data: null}`
- 403 — 账号已禁用 `{code: 100102, message: "账号已被禁用", data: null}`

---

## POST /refresh

**Access**: 匿名（用 refresh token）

**Request**:
```json
{
  "refreshToken": "dGhpcyBp..."
}
```

**Success Response** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "bmV3IHJl...",
    "expiresIn": 900
  },
  "timestamp": "2026-05-11T10:15:00+08:00"
}
```

**Error Response**:
- 401 — 刷新令牌无效或已失效 `{code: 100103, message: "刷新令牌无效或已过期", data: null}`

---

## POST /logout

**Access**: 需认证（`Authorization: Bearer <accessToken>`）

**Request**: (空 body)

**Success Response** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": "2026-05-11T10:30:00+08:00"
}
```

---

## GET /me

**Access**: 需认证

**Success Response** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "roles": ["ADMIN"]
  },
  "timestamp": "2026-05-11T10:00:00+08:00"
}
```

## 通用认证错误

所有需认证的端点，缺少或无效 token 时返回:

```json
{
  "code": 100101,
  "message": "未认证或令牌已过期",
  "data": null,
  "timestamp": "2026-05-11T10:00:00+08:00"
}
```

## 通用说明

- 时区通过 `X-Timezone` 请求头传递（默认 UTC）
- 所有响应使用 `ApiResponse<T>` 统一格式
- 错误码格式 `MMSSEE`：认证模块(10) + 子域(01) + 错误编号
