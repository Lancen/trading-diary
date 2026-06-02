export function fmtAmount(val: number | null): string {
  if (val == null) return "-";
  return (val / 1e8).toFixed(2) + "亿";
}

export function fmtAmountSmart(val: number | null): string {
  if (val == null) return "-";
  if (Math.abs(val) >= 1e8) return (val / 1e8).toFixed(2) + "亿";
  if (Math.abs(val) >= 1e4) return (val / 1e4).toFixed(2) + "万";
  return val.toFixed(2);
}

export function fmtChange(val: number | null): string {
  if (val == null) return "-";
  const sign = val > 0 ? "+" : "";
  return sign + val.toFixed(2) + "%";
}

export function fmtChangeAbs(val: number | null): string {
  if (val == null) return "-";
  const sign = val > 0 ? "+" : "";
  if (Math.abs(val) >= 1e8) return sign + (val / 1e8).toFixed(2) + "亿";
  if (Math.abs(val) >= 1e4) return sign + (val / 1e4).toFixed(2) + "万";
  return sign + val.toFixed(2);
}

export function fmtNum(v: number | undefined | null, decimals = 2): string {
  if (v == null) return "-";
  return v.toLocaleString("zh-CN", { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
}

export function fmtDiff(v: number | undefined | null): string {
  if (v == null) return "-";
  const sign = v >= 0 ? "+" : "";
  return sign + v.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function fmtYi(val: number | null, unit = "亿"): string {
  if (val == null) return "-";
  return (val / 1e8).toFixed(2) + unit;
}

export function fmtWan(val: number | null | undefined, unit: string = ""): string {
  if (val == null) return "-";
  if (unit === "亿") return (val / 1e8).toFixed(2) + "亿";
  if (unit === "万") return (val / 1e4).toFixed(0) + "万";
  return val.toLocaleString();
}

export function diffColor(v: number | undefined | null): string {
  if (v == null) return "text-gray-400";
  return v > 0 ? "text-red-600" : v < 0 ? "text-green-600" : "text-gray-500";
}

export function changeColor(val: number | null): string {
  if (val == null) return "text-gray-400";
  return val > 0 ? "text-red-600" : val < 0 ? "text-green-600" : "text-gray-500";
}

export function isStale(snapDate: string | null): boolean {
  if (!snapDate) return true;
  const d = new Date(snapDate);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  return diffMs > 30 * 24 * 60 * 60 * 1000;
}

export function fmtDate(snapDate: string | null): string {
  if (!snapDate) return "从未采集";
  return snapDate.slice(0, 10);
}

export function formatTime(iso: string | null): string {
  if (!iso) return "-";
  return new Date(iso).toLocaleString("zh-CN", {
    month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit",
  });
}
