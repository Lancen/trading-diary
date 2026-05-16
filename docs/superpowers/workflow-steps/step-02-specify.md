# Step 2: 规范（speckit-specify）

**执行**: `/speckit-specify <"功能描述" 章节的纯 WHAT 文本>`

## 输入

从 design doc "功能描述" 章节**复制文本**，不要传整个文件路径。传文件路径会导致技术细节渗入 spec。

## 产出

- `spec.md` — 用户故事（P1/P2/P3 优先级）、验收场景、功能需求（FR-xxx）、成功标准（SC-xxx）、边界场景、关键实体、假设条件
- `checklists/requirements.md` — 质量检查清单

## 注意

speckit 模板的章节标题已翻译为中文，但 AI 指令注释和格式标记（`[P]`、`[US1]`、FR-/SC- 前缀）保持英文。
