"use client";

import { useCallback, useEffect, useState } from "react";
import api from "@/lib/api";
import KlineMarginOverlay, { KlinePoint, MarginPoint } from "@/components/chart/KlineMarginOverlay";

interface IndexQuote {
  indexCode: string;
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  changePct: number;
}

interface MarketIndexDaily {
  indexCode: string;
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  amount: number;
  changePct: number;
}

interface MacroMargin {
  tradeDate: string;
  marginBalance: number;
  shortBalance: number;
}

const INDEX_NAMES: Record<string, string> = {
  "sh000001": "上证指数",
  "sz399001": "深证成指",
  "sz399006": "创业板指",
  "sh000300": "沪深300",
  "sh000905": "中证500",
  "sh000016": "上证50",
  "sh000688": "科创50",
  "sh000852": "中证1000",
};

const P0_INDICES = ["sh000001", "sz399001", "sz399006", "sh000300", "sh000905"];

export default function IndexAnalysisPage() {
  const [selectedIndex, setSelectedIndex] = useState("sh000001");
  const [latestQuotes, setLatestQuotes] = useState<IndexQuote[]>([]);
  const [klines, setKlines] = useState<KlinePoint[]>([]);
  const [margins, setMargins] = useState<MarginPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [range, setRange] = useState("3m");
  const [period, setPeriod] = useState<"daily" | "weekly" | "monthly">("daily");

  const fetchLatest = useCallback(async () => {
    try {
      const res = await api.get("api/v1/admin/market-index-daily/latest")
        .json<{ code: number; data: MarketIndexDaily[] }>();
      const data = res.data || [];
      setLatestQuotes(data.map(d => ({
        indexCode: d.indexCode,
        tradeDate: d.tradeDate,
        open: d.open,
        high: d.high,
        low: d.low,
        close: d.close,
        volume: d.volume,
        changePct: d.changePct,
      })));
    } catch (e) { console.error(e); }
  }, []);

  const fetchChart = useCallback(async () => {
    setLoading(true);
    try {
      const now = new Date();
      let startDate = "";
      if (range === "1m") { const d = new Date(now); d.setMonth(d.getMonth() - 1); startDate = d.toISOString().slice(0, 10); }
      else if (range === "3m") { const d = new Date(now); d.setMonth(d.getMonth() - 3); startDate = d.toISOString().slice(0, 10); }
      else if (range === "6m") { const d = new Date(now); d.setMonth(d.getMonth() - 6); startDate = d.toISOString().slice(0, 10); }
      else if (range === "1y") { const d = new Date(now); d.setFullYear(d.getFullYear() - 1); startDate = d.toISOString().slice(0, 10); }

      const klineRes = await api.get("api/v1/admin/market-index-daily", {
        searchParams: { indexCode: selectedIndex, startDate },
      }).json<{ code: number; data: MarketIndexDaily[] }>();

      setKlines((klineRes.data || []).map(k => ({
        tradeDate: k.tradeDate, open: k.open, high: k.high, low: k.low, close: k.close, volume: k.volume,
      })));

      try {
        const marginRes = await api.get("api/v1/admin/margin-macro/sse", {
          searchParams: { startDate },
        }).json<{ code: number; data: MacroMargin[] }>();
        setMargins((marginRes.data || []).map(m => ({
          tradeDate: m.tradeDate, marginBalance: m.marginBalance, shortBalance: m.shortBalance,
        })));
      } catch {
        setMargins([]);
      }
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, [selectedIndex, range]);

  useEffect(() => { fetchLatest(); }, [fetchLatest]);
  useEffect(() => { fetchChart(); }, [fetchChart]);

  const currentQuote = latestQuotes.find(q => q.indexCode === selectedIndex);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">指数分析</h1>

      {/* P0 指数概览卡片 */}
      <div className="grid grid-cols-5 gap-3">
        {P0_INDICES.map(code => {
          const q = latestQuotes.find(q => q.indexCode === code);
          const isUp = (q?.changePct ?? 0) > 0;
          return (
            <button
              key={code}
              onClick={() => setSelectedIndex(code)}
              className={`rounded-lg border p-3 text-left transition-colors ${selectedIndex === code ? "border-blue-500 bg-blue-50" : "hover:bg-gray-50"}`}
            >
              <p className="text-xs text-gray-500">{INDEX_NAMES[code] || code}</p>
              <p className={`text-lg font-bold ${isUp ? "text-red-600" : "text-green-600"}`}>{q?.close?.toFixed(2) ?? "-"}</p>
              <p className={`text-xs ${isUp ? "text-red-500" : "text-green-500"}`}>{q?.changePct != null ? (q.changePct > 0 ? "+" : "") + q.changePct.toFixed(2) + "%" : "-"}</p>
            </button>
          );
        })}
      </div>

      {/* 指数列表 */}
      <div className="rounded-lg border">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-gray-50 text-xs text-gray-500">
              {["指数", "收盘", "涨跌幅", "成交量"].map(h => <th key={h} className="px-3 py-2 text-right">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {Object.entries(INDEX_NAMES).map(([code, name]) => {
              const q = latestQuotes.find(q => q.indexCode === code);
              const isUp = (q?.changePct ?? 0) > 0;
              return (
                <tr key={code} className={`border-b hover:bg-gray-50 cursor-pointer ${selectedIndex === code ? "bg-blue-50" : ""}`} onClick={() => setSelectedIndex(code)}>
                  <td className="px-3 py-2 font-medium">{name}</td>
                  <td className={`px-3 py-2 text-right ${isUp ? "text-red-600" : "text-green-600"}`}>{q?.close?.toFixed(2) ?? "-"}</td>
                  <td className={`px-3 py-2 text-right ${isUp ? "text-red-600" : "text-green-600"}`}>{q?.changePct != null ? (q.changePct > 0 ? "+" : "") + q.changePct.toFixed(2) + "%" : "-"}</td>
                  <td className="px-3 py-2 text-right">{q?.volume ? (q.volume / 1e8).toFixed(2) + "亿" : "-"}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* K线+两融叠加图 */}
      <div className="rounded-lg border p-4">
        <div className="flex flex-wrap gap-2 items-center mb-3">
          <span className="font-semibold text-sm">{INDEX_NAMES[selectedIndex] || selectedIndex}</span>
          <span className="text-gray-300">|</span>
          {["1m", "3m", "6m", "1y"].map((r) => <button key={r} onClick={() => setRange(r)} className={`rounded px-2 py-1 text-xs ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{r === "1m" ? "1月" : r === "3m" ? "3月" : r === "6m" ? "6月" : "1年"}</button>)}
          <span className="text-gray-300">|</span>
          {(["daily", "weekly", "monthly"] as const).map((p) => <button key={p} onClick={() => setPeriod(p)} className={`rounded px-2 py-1 text-xs ${period === p ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{p === "daily" ? "日K" : p === "weekly" ? "周K" : "月K"}</button>)}
        </div>
        {loading ? <div className="h-[380px] flex items-center justify-center text-gray-500">加载中...</div> : <KlineMarginOverlay klines={klines} margins={margins} period={period} />}
      </div>
    </div>
  );
}
