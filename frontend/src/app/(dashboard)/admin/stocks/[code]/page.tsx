"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import api from "@/lib/api";
import { CandlestickData, CandlestickSeries, ColorType, createChart, CrosshairMode, HistogramData, HistogramSeries, LineData, LineSeries, Time } from "lightweight-charts";

interface Kline { tradeDate: string; open: number; high: number; low: number; close: number; volume: number; }
interface Margin { tradeDate: string; marginBalance: number; marginChange: number; shortBalance: number; shortChange: number; }
interface DetailData {
  stockCode: string; stockName: string; industry: string; concepts: string[];
  latestQuote: { open: number; high: number; low: number; close: number; volume: number; changePct: number; };
  latestMargin: { marginBalance: number; marginBuy: number; shortBalance: number; totalBalance: number; } | null;
  dailyKlines: Kline[];
  dailyMargins: Margin[];
}

function getWeekKey(d: string): string {
  const date = new Date(d);
  const day = date.getDay();
  const monday = new Date(date);
  monday.setDate(date.getDate() - (day === 0 ? 6 : day - 1));
  return monday.toISOString().slice(0, 10);
}

function getMonthKey(d: string): string { return d.slice(0, 7); }

function aggregateKlines(klines: Kline[], period: "daily" | "weekly" | "monthly"): Kline[] {
  if (period === "daily") return klines;
  const getKey = period === "weekly" ? getWeekKey : getMonthKey;
  const groups = new Map<string, Kline[]>();
  for (const k of klines) {
    const key = getKey(k.tradeDate);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(k);
  }
  return Array.from(groups.entries()).map(([key, bars]) => ({
    tradeDate: bars[bars.length - 1].tradeDate,
    open: bars[0].open,
    high: Math.max(...bars.map(b => b.high)),
    low: Math.min(...bars.map(b => b.low)),
    close: bars[bars.length - 1].close,
    volume: bars.reduce((s, b) => s + b.volume, 0),
  }));
}

export default function StockDetailPage() {
  const { code } = useParams<{ code: string }>();
  const router = useRouter();
  const chartRef = useRef<HTMLDivElement>(null);
  const [detail, setDetail] = useState<DetailData | null>(null);
  const [loading, setLoading] = useState(true);
  const [range, setRange] = useState("3m");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [period, setPeriod] = useState<"daily" | "weekly" | "monthly">("daily");
  const [showTable, setShowTable] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      const res = await api.get(`api/v1/admin/stocks/${code}`, { searchParams: params }).json<{ code: number; data: DetailData }>();
      setDetail(res.data || null);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, [code, startDate, endDate]);

  useEffect(() => { fetchData(); }, [fetchData]);

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

  useEffect(() => {
    if (!detail?.dailyKlines?.length || !chartRef.current) return;
    const container = chartRef.current;
    const chart = createChart(container, {
      layout: { background: { type: ColorType.Solid, color: "#fff" }, textColor: "#6b7280" },
      rightPriceScale: { visible: true, borderColor: "#e5e7eb" },
      crosshair: { mode: CrosshairMode.Normal },
      timeScale: { borderColor: "#e5e7eb" },
    });

    const klines = aggregateKlines(detail.dailyKlines, period);

    const candleData: CandlestickData<Time>[] = klines.map((k) => ({ time: k.tradeDate as Time, open: k.open, high: k.high, low: k.low, close: k.close }));
    const volumeData: HistogramData<Time>[] = klines.map((k) => ({ time: k.tradeDate as Time, value: k.volume, color: k.close >= k.open ? "rgba(34,197,94,0.3)" : "rgba(239,68,68,0.3)" }));

    const candleSeries = chart.addSeries(CandlestickSeries, { upColor: "#dc2626", downColor: "#16a34a", borderUpColor: "#dc2626", borderDownColor: "#16a34a", wickUpColor: "#dc2626", wickDownColor: "#16a34a" });
    candleSeries.setData(candleData);

    const volSeries = chart.addSeries(HistogramSeries, { priceFormat: { type: "volume" }, priceScaleId: "" });
    volSeries.setData(volumeData);
    volSeries.priceScale().applyOptions({ scaleMargins: { top: 0.85, bottom: 0 } });

    if (detail.dailyMargins?.length) {
      const marginMap = new Map(detail.dailyMargins.map((m) => [m.tradeDate, m]));
      const marginBalanceData: LineData<Time>[] = klines.map((k) => ({ time: k.tradeDate as Time, value: marginMap.get(k.tradeDate)?.marginBalance ?? undefined })).filter((d): d is LineData<Time> => d.value != null);
      const shortBalanceData: LineData<Time>[] = klines.map((k) => ({ time: k.tradeDate as Time, value: marginMap.get(k.tradeDate)?.shortBalance ?? undefined })).filter((d): d is LineData<Time> => d.value != null);

      const marginSeries = chart.addSeries(LineSeries, { priceScaleId: "margin", color: "#2563eb", lineWidth: 1 });
      marginSeries.setData(marginBalanceData);

      const shortSeries = chart.addSeries(LineSeries, { priceScaleId: "margin", color: "#7c3aed", lineWidth: 1, lineStyle: 2 });
      shortSeries.setData(shortBalanceData);
    }

    chart.timeScale().fitContent();
    return () => chart.remove();
  }, [detail, period]);

  if (loading) return <div className="py-8 text-center text-gray-500">加载中...</div>;
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
        <div ref={chartRef} className="h-[380px]" />
        <div className="flex gap-4 mt-2 text-xs text-gray-500">
          <span>🕯️ K线</span><span>📊 成交量</span><span className="text-blue-600">━━ 融资余额(右轴)</span><span className="text-purple-600">╌╌ 融券余额(右轴)</span>
        </div>
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
