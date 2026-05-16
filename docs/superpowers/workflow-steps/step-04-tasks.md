# Step 4: 拆解（speckit-tasks）

**执行**: `/speckit-tasks`

## 产出

`tasks.md` — 按 User Story 分组的任务列表，标注依赖关系和并行标记 [P]。

## 局限认知

tasks.md 按 spec 的用户故事分组，不代表代码的真实耦合关系。例如 login/refresh/logout 在 spec 中是两个 User Story，但在代码中是同一个 AuthService/AuthController 实现。执行时相邻阶段可能自然合并——这是正常的，不是 tasks.md 的质量问题。
