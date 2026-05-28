# 前端开发规范

## API 通信
- 所有后端 API 调用必须通过 `src/lib/api.ts`（ky 实例），禁止直接使用 fetch/axios
- `useApiQuery<T>` 和 `useApiMutation<TData, TVariables>` 是唯一的数据获取/变更方式，禁止 useState+useEffect 手动管理请求
- Query Key 必须使用 `lib/hooks.ts` 的 `keys` 工厂函数，禁止内联硬编码数组
- `useApiQuery` 的泛型 `<T>` 对应 `ApiResponse<T>` 中 `data` 字段的类型
- searchParams 中 undefined 值会被 `filterParams` 自动过滤，无需手动清除

## 数据获取模式
```typescript
// ✅ 合规：使用 useApiQuery
const { data, isLoading } = useApiQuery<SectorItem[]>(
  keys.sectors("concepts"),
  "api/v1/admin/market/concepts"
);
const items = data?.data || [];

// ❌ 违规：useState + useEffect + fetch
const [data, setData] = useState([]);
useEffect(() => { fetch(...).then(setData); }, []);
```

## 类型与格式化
- 业务类型集中定义在 `lib/types.ts`，按领域分块（注释分隔线 + 原出处标注）
- 格式化函数集中定义在 `lib/format.ts`，签名统一为 `(value: number | null | undefined) => string`
- 禁止在页面组件中定义内联 `fmt`/`diffColor`/`isStale` 等重复工具函数
- 颜色函数零值特殊处理：`diffColor` 和 `changeColor` 对零值返回 `text-foreground`

## 参数化组件（去近克隆）
- 概念/行业板块存在近克隆时，提取参数化组件 + `SectorTypeConfig` 配置对象
- 页面文件只做配置注入，不含业务逻辑（5 行以内）
- 配置驱动 4 个维度：`apiPath`、`queryKey`、`detailRoute`、`title`
- 新增板块类型（如地域板块）只需新增一个 `Xxx_CONFIG` 常量

## 页面壳模式
```typescript
// concepts/page.tsx — 只做委托
"use client";
import SectorListPage, { CONCEPT_CONFIG } from "@/components/SectorListPage";
export default function ConceptsPage() {
  return <SectorListPage config={CONCEPT_CONFIG} />;
}
```

## 组件目录
| 目录 | 用途 | 命名 |
|------|------|------|
| `src/components/` | 业务组件 | PascalCase |
| `src/components/chart/` | 图表业务组件 | PascalCase |
| `src/components/ui/` | shadcn/ui 基础组件 | kebab-case（保持上游） |
| `src/lib/` | 工具函数/hooks/类型 | camelCase |

## Badge 使用规则
- 项目未安装 shadcn/ui Badge 组件，禁止 `import { Badge } from '@/components/ui/badge'`
- 需要状态标签时，在页面内定义轻量内联 Badge（variant 映射 Tailwind 类名）
- 配合 `format.ts` 的 `statusVariant()` 和 `statusLabel()` 使用

## 路由与认证
- 新页面必须放在 `(dashboard)` 路由组下，受 AuthGuard 保护
- 登录页放在 `app/login/` 下，不受 AuthGuard 保护
- 开发模式自动登录由 `useAuth.isDev` + 后端 `AutoLoginFilter` 联合处理

## 状态管理
- 认证状态必须通过 `useAuth` (Zustand store) 管理，禁止组件自行管理 token
- 服务端数据优先使用 TanStack React Query，全局状态使用 Zustand