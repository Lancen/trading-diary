/**
 * 前端共享格式化工具，从各页面重复的辅助函数抽取合并
 */

// ============================================================
// 数值格式化
// ============================================================

/**
 * 除以 1 亿并附加"亿"后缀，用于大额数值显示
 * 例: 1234567890 → "12.35 亿"
 */
export function fmt(value: number | null | undefined): string {
  if (value == null) return "-";
  return (value / 1e8).toFixed(2) + " 亿";
}

/**
 * 千分位格式化，用于整数/小数显示
 * 例: 1234567 → "1,234,567"
 */
export function fmtNum(value: number | null | undefined): string {
  if (value == null) return "-";
  return value.toLocaleString("zh-CN");
}

/**
 * 带正负号的数值格式化，正数前加"+"
 * 例: 1234 → "+1,234", -567 → "-567"
 */
export function fmtDiff(value: number | null | undefined): string {
  if (value == null) return "-";
  const formatted = value.toLocaleString("zh-CN");
  return value > 0 ? `+${formatted}` : formatted;
}

/**
 * 排名页成交额格式化：除以 1 亿保留两位小数 + "亿"
 * 与 fmt() 相同逻辑，语义化别名
 */
export function fmtAmount(value: number | null | undefined): string {
  if (value == null) return "-";
  return (value / 1e8).toFixed(2) + "亿";
}

/**
 * 排名页涨跌幅格式化：带正负号 + "%"
 * 例: 2.5 → "+2.50%", -1.3 → "-1.30%"
 */
export function fmtChange(value: number | null | undefined): string {
  if (value == null) return "-";
  const fixed = value.toFixed(2);
  return value > 0 ? `+${fixed}%` : `${fixed}%`;
}

/**
 * 排名页涨跌幅绝对值格式化 + "%"
 * 例: 2.5 → "2.50%"
 */
export function fmtChangeAbs(value: number | null | undefined): string {
  if (value == null) return "-";
  return value.toFixed(2) + "%";
}

/**
 * 成交占比格式化：乘 100 保留两位小数 + "%"
 * 用于将小数比例（0-1）转为百分比显示
 * 例: 0.1523 → "15.23%"
 */
export function fmtVolumePct(value: number | null | undefined): string {
  if (value == null) return "-";
  return (value * 100).toFixed(2) + "%";
}

// ============================================================
// 颜色样式
// ============================================================

/**
 * 根据数值正负返回红/绿色 Tailwind 类名
 * 正值 → 红色（涨），负值 → 绿色（跌），零 → 默认色
 */
export function diffColor(value: number | null | undefined): string {
  if (value == null || value === 0) return "text-foreground";
  return value > 0 ? "text-red-500" : "text-green-500";
}

/**
 * 根据涨跌幅返回颜色类名（专用于 changePct 字段）
 */
export function changeColor(value: number | null | undefined): string {
  if (value == null || value === 0) return "text-foreground";
  return value > 0 ? "text-red-500" : "text-green-500";
}

// ============================================================
// 日期/时间
// ============================================================

/**
 * 判断快照日期是否过期（距今天超过 30 天）
 */
export function isStale(snapDate: string | null | undefined): boolean {
  if (!snapDate) return true;
  const diff = Date.now() - new Date(snapDate).getTime();
  return diff > 30 * 24 * 60 * 60 * 1000;
}

/**
 * 格式化快照日期为 "M/D" 格式
 * 例: "2025-05-27" → "5/27"
 */
export function fmtDate(dateStr: string | null | undefined): string {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

/**
 * 格式化时间戳为简短时间字符串 "HH:mm"
 * 用于采集日志的 startedAt / completedAt 显示
 */
export function formatTime(isoStr: string | null | undefined): string {
  if (!isoStr) return "-";
  const d = new Date(isoStr);
  const h = String(d.getHours()).padStart(2, "0");
  const m = String(d.getMinutes()).padStart(2, "0");
  return `${h}:${m}`;
}

/**
 * 根据时间范围快捷选项计算起始日期
 * @param range - "1m" / "3m" / "6m" / "1y"
 * @returns ISO 格式的起始日期字符串 (yyyy-MM-dd)
 */
export function getStartDate(range: "1m" | "3m" | "6m" | "1y"): string {
  const now = new Date();
  const months: Record<string, number> = { "1m": 1, "3m": 3, "6m": 6, "1y": 12 };
  now.setMonth(now.getMonth() - months[range]);
  return now.toISOString().split("T")[0];
}

// ============================================================
// 采集状态
// ============================================================

/**
 * 采集状态对应的 badge 颜色变体
 */
export function statusVariant(
  status: string | null | undefined
): "default" | "secondary" | "destructive" | "outline" {
  switch (status) {
    case "COMPLETED":
      return "default";
    case "RUNNING":
      return "secondary";
    case "FAILED":
      return "destructive";
    default:
      return "outline";
  }
}

/**
 * 采集状态中文名
 */
export function statusLabel(status: string | null | undefined): string {
  switch (status) {
    case "COMPLETED":
      return "完成";
    case "RUNNING":
      return "运行中";
    case "FAILED":
      return "失败";
    case "PENDING":
      return "等待中";
    default:
      return "未运行";
  }
}

/**
 * 数据类型中文名
 */
export function dataTypeLabel(dataType: string): string {
  const labels: Record<string, string> = {
    STOCK_INFO: "股票行情",
    STOCK_DAILY: "股票日线",
    MARGIN_DAILY_SSE: "沪市两融明细",
    MARGIN_DAILY_SZSE: "深市两融明细",
    MARGIN_MACRO_SSE: "沪市两融总量",
    MARGIN_MACRO_SZSE: "深市两融总量",
    TRADE_CALENDAR: "交易日历",
    INDUSTRY_NAME: "行业列表",
    CONCEPT_NAME: "概念列表",
  };
  return labels[dataType] ?? dataType;
}