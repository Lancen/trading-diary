# Step 8: 归档（archive）

**执行**: `/superpowers:finishing-a-development-branch`

## 前置检查

- [ ] 步骤 7 全部 Phase 通过，收尾审查 ✅
- [ ] verify（AC 验收）通过
- [ ] 全部测试通过（单元 + 集成 + E2E）
- [ ] 编译通过

## 归档操作

### 1. 更新 feature 状态

将 `specs/_feature-status.md` 对应 feature 的 `状态` 改为 `done`（全部 ✅）。

### 2. 移动 spec 到归档目录

```
specs/003-data-collection/  →  specs/_archive/003-data-collection-2026-05-XX/
```

归档目录名包含日期，便于回溯。保留 design doc 路径不变（design doc 在 `docs/superpowers/specs/` 下，是长期参考文档）。

### 3. 创建归档记录

在归档目录中生成 `ARCHIVE.md`：

```markdown
# 003-data-collection 归档记录

- 完成日期: 2026-05-XX
- 提交范围: commit-abc..commit-xyz
- 主要变更: (一句话摘要)
```

### 4. 提交并合并

```bash
git add specs/_archive/ specs/_feature-status.md
git commit -m "归档 003-data-collection —— (摘要)"
```

然后按团队流程合并到 main。

## 设计决策

- **为什么原地移动而非复制**：spec 目录移走后 `specs/` 根干净，下一个 feature 直接进去看到的就是当前工作的 spec
- **为什么保留 design doc 不动**：design doc 是项目长期技术参考，不是一次性交付物
- **为什么不是独立命令**：归档依赖人工判断（确认可以合并），不适合全自动
