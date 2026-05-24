# Service 必须使用接口+实现类模式

所有 Service 必须定义为接口+实现类，禁止直接使用具体类注册为 Spring Bean。

## 规则

1. **接口定义**：在 Service 包下定义接口，包含所有 public 方法签名和 Javadoc
2. **实现类**：在 `impl/` 子包下创建实现类，添加 `@Service` 注解
3. **依赖注入**：所有调用方（Controller、其他 Service、测试）必须注入接口类型，不直接引用实现类

## 命名约定

| 接口 | 实现类 |
|------|--------|
| `FooService` | `impl/FooServiceImpl` |

## 目录结构

```
service/
├── AuthService.java              # 接口
├── impl/
│   └── AuthServiceImpl.java      # 实现（@Service）
├── collection/
│   ├── CollectionQueryService.java    # 接口
│   ├── MarginCleanseService.java      # 接口
│   └── impl/
│       ├── CollectionQueryServiceImpl.java  # 实现（@Service）
│       └── MarginCleanseServiceImpl.java    # 实现（@Service）
```

## Why

- 接口是依赖倒置的核心，调用方依赖抽象而非实现
- 便于 Mock 测试（Mockito mock 接口比 mock 具体类更干净）
- 为未来多实现、AOP 代理、远程调用预留扩展点

## How to apply

- 新增 Service → 先写接口再写实现
- 重构现有 Service → 提取接口，实现类移入 `impl/` 子包，更新所有注入点
