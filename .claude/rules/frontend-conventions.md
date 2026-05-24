# 前端开发规范

## API 通信
- 所有后端 API 调用必须通过 `src/lib/api.ts`（ky 实例），禁止直接使用 fetch/axios
- API 函数统一放在 `src/lib/auth.ts` 或页面级组件中

## 路由与认证
- 新页面必须放在 `(dashboard)` 路由组下，受 AuthGuard 保护
- 登录页放在 `app/login/` 下，不受 AuthGuard 保护
- 开发模式自动登录由 `useAuth.isDev` + 后端 `AutoLoginFilter` 联合处理

## 状态管理
- 认证状态必须通过 `useAuth` (Zustand store) 管理，禁止组件自行管理 token
- 服务端数据优先使用 TanStack React Query，全局状态使用 Zustand

## 组件
- UI 基础组件使用 shadcn/ui（`components/ui/`），保持上游命名约定（kebab-case）
- 业务组件使用 PascalCase 命名，放在 `components/` 对应子目录下
- 页面组件放在 `app/` 对应路由目录下
