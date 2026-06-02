"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useApiQuery, keys } from "@/lib/hooks";
import type { DetailData } from "@/lib/types";
import KlineMarginOverlay, { KlinePoint, MarginPoint } from "@/components/chart/KlineMarginOverlay";

export default function StockDetailPage() {
  const { code } = useParams<{ code: string }>();
  const router = useRouter();
  const [range, setRange] = useState("3m");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [period, setPeriod] = useState<"daily" | "weekly" | "monthly">("daily");
  const [showTable, setShowTable] = useState(false);

  const { data: detail, isPending, error } = useApiQuery<DetailData>(
    keys.stockDetail(code, { startDate, endDate }),
    `api/v1/admin/stocks/${code}`,
    { startDate: startDate || undefined, endDate: endDate || undefined },
    { enabled: !!startDate },
  );

  function applyRange(r: string) {
    setRange(r);
    const now = new Date();
    const end = now.toISOString().slice(0, 10);
    let start = end;
    if (r === "1m") { const d = new Date(now); d.setMonth(d.getMonth() - 1); start = d.toISOString().slice(0, 10); }
    else if (r === "3m") { const d = new Date(now); d.setMonth(d.getMonth() - 3); start = d.toISOString().slice(0, 10); }
    else if (r === "6m") { const d = new Date(now); d.setMonth(d.getMonth() - 6); start = d.toISOString().slice(0, 10); }
    else if (r === "1y") { const d = new Date(now); d.setFullYear(d.getFullYear() - 1); start = d.toISOString().slice(0, 10); }
    setStartDate(start);
    setEndDate(end);
  }

  useEffect(() => { applyRange("3m"); }, []);

  const klinePoints: KlinePoint[] = (detail?.dailyKlines || []).map(k => ({
    tradeDate: k.tradeDate, open: k.open, high: k.high, low: k.low, close: k.close, volume: k.volume,
  }));

  const marginPoints: MarginPoint[] = (detail?.dailyMargins || []).map(m => ({
    tradeDate: m.tradeDate, marginBalance: m.marginBalance, shortBalance: m.shortBalance,
  }));

  if (isPending && !error) return <div className="py-8 text-center text-gray-500">加载中...</div>;
  if (!detail) return <div className="py-8 text-center text-gray-500">未找到该股票</div>;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <button onClick={() => router.back()} className="hover:underline">← 返回股票数据</button>
        <span className="text-gray-300">|</span>
        <h1 className="text-2xl font-bold text-gray-900">{detail.stockCode} {detail.stockName}</h1>
      </div>

      {/* 行业+概念 */}
      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-lg border p-3"><span className="text-xs text-gray-500">所属行业</span><p className="font-medium">{detail.industry || "-"}</p></div>
        <div className="rounded-lg border p-3"><span className="text-xs text-gray-500">所属概念</span><div className="flex flex-wrap gap-1 mt-1">{(detail.concepts || []).map((c) => <span key={c} className="rounded bg-purple-50 px-2 py-0.5 text-xs text-purple-700">{c}</span>)}</div></div>
      </div>

      {/* 行情摘要 */}
      {detail.latestQuote && (
        <div className="grid grid-cols-6 gap-3">
          {["收盘", "开盘", "最高", "最低", "成交量", "涨跌幅"].map((label, i) => {
            const vals = [detail.latestQuote?.close, detail.latestQuote?.open, detail.latestQuote?.high, detail.latestQuote?.low, `${(detail.latestQuote?.volume || 0 / 1e4).toFixed(0)}万`, `${detail.latestQuote?.changePct?.toFixed(2)}%`];
            const colors = i === 5 && detail.latestQuote?.changePct > 0 ? "text-red-600" : "";
            return <div key={label} className="rounded-lg bg-gray-50 p-3 text-center"><p className="text-xs text-gray-500">{label}</p><p className={`text-lg font-bold ${colors}`}>{vals[i]}</p></div>;
          })}
        </div>
      )}

      {/* K线图 */}
      <div className="rounded-lg border p-4">
        <div className="flex flex-wrap gap-2 items-center mb-3">
          <span className="font-semibold text-sm">K线图</span>
          {["1m", "3m", "6m", "1y"].map((r) => <button key={r} onClick={() => applyRange(r)} className={`rounded px-2 py-1 text-xs ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{r === "1m" ? "1月" : r === "3m" ? "3月" : r === "6m" ? "6月" : "1年"}</button>)}
          <span className="text-gray-300">|</span>
          <input type="date" value={startDate} onChange={(e) => { setStartDate(e.target.value); setRange(""); }} className="rounded border px-2 py-1 text-xs w-32" />
          <span className="text-xs">~</span>
          <input type="date" value={endDate} onChange={(e) => { setEndDate(e.target.value); setRange(""); }} className="rounded border px-2 py-1 text-xs w-32" />
          <span className="text-gray-300">|</span>
          {(["daily", "weekly", "monthly"] as const).map((p) => <button key={p} onClick={() => setPeriod(p)} className={`rounded px-2 py-1 text-xs ${period === p ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{p === "daily" ? "日K" : p === "weekly" ? "周K" : "月K"}</button>)}
        </div>
        <KlineMarginOverlay klines={klinePoints} margins={marginPoints} period={period} />
      </div>

      {/* 两融数据 */}
      {detail.latestMargin && (
        <div className="rounded-lg border p-4">
          <h3 className="font-semibold text-sm mb-2">两融数据（最新）</h3>
          <div className="grid grid-cols-4 gap-3">
            <div className="rounded-lg bg-gray-50 p-3 text-center"><p className="text-xs text-gray-500">融资余额</p><p className="text-base font-bold">{(detail.latestMargin.marginBalance / 1e8).toFixed(2)}亿</p></div>
            <div className="rounded-lg bg-gray-50 p-3 text-center"><p className="text-xs text-gray-500">融资买入</p><p className="text-base font-bold">{(detail.latestMargin.marginBuy / 1e8).toFixed(2)}亿</p></div>
            <div className="rounded-lg bg-gray-50 p-3 text-center"><p className="text-xs text-gray-500">融券余额</p><p className="text-base font-bold">{(detail.latestMargin.shortBalance / 1e8).toFixed(2)}亿</p></div>
            <div className="rounded-lg bg-gray-50 p-3 text-center"><p className="text-xs text-gray-500">两融总余额</p><p className="text-base font-bold">{(detail.latestMargin.totalBalance / 1e8).toFixed(2)}亿</p></div>
          </div>
        </div>
      )}

      {/* 日线明细 */}
      <button onClick={() => setShowTable(!showTable)} className="text-sm text-blue-600 hover:underline">{showTable ? "收起日线明细 ▲" : "展开日线明细 ▼"}</button>
      {showTable && detail.dailyKlines.length > 0 && (
        <div className="overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead><tr className="border-b bg-gray-50 text-xs text-gray-500">{["日期","开盘","收盘","最高","最低","成交量","涨跌幅"].map(h => <th key={h} className="px-3 py-2 text-right">{h}</th>)}</tr></thead>
            <tbody>
              {detail.dailyKlines.map((k, i) => {
                const prev = detail.dailyKlines[i + 1];
                const pct = prev ? ((k.close - prev.close) / prev.close * 100).toFixed(2) : "-";
                return <tr key={k.tradeDate} className="border-b hover:bg-gray-50"><td className="px-3 py-1">{k.tradeDate}</td><td className="px-3 py-1 text-right">{k.open}</td><td className="px-3 py-1 text-right">{k.close}</td><td className="px-3 py-1 text-right">{k.high}</td><td className="px-3 py-1 text-right">{k.low}</td><td className="px-3 py-1 text-right">{k.volume}</td><td className={`px-3 py-1 text-right ${typeof pct === "string" ? "" : +pct > 0 ? "text-red-600" : "text-green-600"}`}>{typeof pct === "string" ? pct : (+pct > 0 ? "+" : "") + pct + "%"}</td></tr>;
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
