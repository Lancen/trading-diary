# 用户界面文本统一中文

所有用户可见文本必须使用中文，禁止中英混杂。

## 前端

JSX 中的显示文本、状态标签、按钮文案、placeholder、空状态提示、表格表头、下拉选项 label 统一用中文。

## 后端

- `ApiResponse.fail()` 的 message → 中文
- `throw new RuntimeException("...")` → 中文（会被前端展示）
- Controller 返回的提示字符串 → 中文

## 例外（可保留英文）

- 应用品牌名（如 "Trading Diary"）
- API 路径、JSON key、URL 参数名
- 代码标识符（变量名、方法名、类名）
- 日志输出（非用户可见）
