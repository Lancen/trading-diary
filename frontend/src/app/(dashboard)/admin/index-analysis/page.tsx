"use client";

import { useState } from "react";
import { useApiQuery } from "@/lib/hooks";
import type { MarketIndexDaily, MacroMargin } from "@/lib/types";
import { fmt, fmtChange, diffColor, getStartDate } from "@/lib/format";
import KlineMarginOverlay from "@/components/chart/KlineMarginOverlay";

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

type RangeKey = "1m" | "3m" | "6m" | "1y";

export default function IndexAnalysisPage() {
  const [selectedIndex, setSelectedIndex] = useState("sh000001");
  const [range, setRange] = useState<RangeKey>("3m");
  const startDate = getStartDate(range);

  const { data: quotesData } = useApiQuery<MarketIndexDaily[]>(
    ["admin", "market-index-daily", "latest"],
    "api/v1/admin/market-index-daily/latest"
  );

  const { data: klineData, isLoading: klineLoading } = useApiQuery<MarketIndexDaily[]>(
    ["admin", "index-analysis", range, selectedIndex],
    "api/v1/admin/market-index-daily",
    { indexCode: selectedIndex, startDate }
  );

  const { data: marginData } = useApiQuery<MacroMargin[]>(
    ["admin", "margin-macro", "sse", range],
    "api/v1/admin/margin-macro/sse",
    { startDate }
  );

  const latestQuotes = quotesData?.data || [];
  const klines = klineData?.data || [];
  const margins = marginData?.data || [];
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
            <button key={code} onClick={() => setSelectedIndex(code)}
              className={`rounded-lg border p-3 text-left transition-colors ${selectedIndex === code ? "border-blue-500 bg-blue-50" : "hover:bg-gray-50"}`}>
              <p className="text-xs text-gray-500">{INDEX_NAMES[code] || code}</p>
              <p className={`text-lg font-bold ${diffColor(q?.changePct)}`}>{q?.close?.toFixed(2) ?? "-"}</p>
              <p className={`text-xs ${diffColor(q?.changePct)}`}>{fmtChange(q?.changePct)}</p>
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
                  <td className={`px-3 py-2 text-right ${diffColor(q?.changePct)}`}>{q?.close?.toFixed(2) ?? "-"}</td>
                  <td className={`px-3 py-2 text-right ${diffColor(q?.changePct)}`}>{fmtChange(q?.changePct)}</td>
                  <td className="px-3 py-2 text-right">{q?.volume ? fmt(q.volume) : "-"}</td>
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
          {(["1m", "3m", "6m", "1y"] as RangeKey[]).map((r) => <button key={r} onClick={() => setRange(r)} className={`rounded px-2 py-1 text-xs ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{r === "1m" ? "1月" : r === "3m" ? "3月" : r === "6m" ? "6月" : "1年"}</button>)}
        </div>
        {klineLoading ? <div className="h-[380px] flex items-center justify-center text-gray-500">加载中...</div> : <KlineMarginOverlay klines={klines} margins={margins} />}
      </div>
    </div>
  );
}