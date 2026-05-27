"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useApiQuery } from "@/lib/hooks";
import type { StockDetailData } from "@/lib/types";
import { fmt, diffColor, getStartDate } from "@/lib/format";
import KlineMarginOverlay from "@/components/chart/KlineMarginOverlay";

type RangeKey = "1m" | "3m" | "6m" | "1y";

export default function StockDetailPage() {
  const params = useParams();
  const code = params.code as string;
  const [range, setRange] = useState<RangeKey>("3m");
  const startDate = getStartDate(range);

  const { data, isLoading } = useApiQuery<StockDetailData>(
    ["admin", "stocks", code],
    `api/v1/admin/stocks/${code}`,
    { startDate }
  );

  const stock = data?.data;
  const quote = stock?.latestQuote;
  const margin = stock?.latestMargin;

  return (
    <div className="space-y-6">
      <a href="/admin/stocks" className="text-sm text-gray-400 hover:text-blue-600">← 股票列表</a>

      {isLoading ? (
        <div className="text-center text-gray-400 py-12">加载中...</div>
      ) : !stock ? (
        <div className="text-center text-gray-400 py-12">无数据</div>
      ) : (
        <>
          <div className="flex items-end gap-4">
            <h1 className="text-2xl font-bold">{stock.stockName}</h1>
            <span className="text-gray-400 font-mono">{stock.stockCode}</span>
          </div>

          {/* 行情摘要 */}
          {quote && (
            <div className="grid grid-cols-5 gap-4">
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">收盘</div>
                <div className={`text-2xl font-bold ${diffColor(quote.changePct)}`}>{quote.close.toFixed(2)}</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">涨跌幅</div>
                <div className={`text-2xl font-bold ${diffColor(quote.changePct)}`}>{quote.changePct > 0 ? "+" : ""}{quote.changePct.toFixed(2)}%</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">今开 / 最高 / 最低</div>
                <div className="text-lg">{quote.open.toFixed(2)} / {quote.high.toFixed(2)} / {quote.low.toFixed(2)}</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">成交量</div>
                <div className="text-lg">{fmt(quote.volume)}</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">行业</div>
                <div className="text-lg">{stock.industry || "-"}</div>
              </div>
            </div>
          )}

          {/* 两融摘要 */}
          {margin && (
            <div className="grid grid-cols-4 gap-4">
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">融资余额</div>
                <div className="text-xl font-bold text-blue-600">{fmt(margin.marginBalance)}</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">融券余额</div>
                <div className="text-xl font-bold text-orange-600">{fmt(margin.shortBalance)}</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">两融总额</div>
                <div className="text-xl font-bold">{fmt(margin.totalBalance)}</div>
              </div>
              <div className="rounded-lg border bg-white p-4">
                <div className="text-xs text-gray-500 mb-1">融资买入</div>
                <div className="text-xl font-bold">{fmt(margin.marginBuy)}</div>
              </div>
            </div>
          )}

          {/* 概念标签 */}
          {stock.concepts.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {stock.concepts.map((c) => (
                <span key={c} className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-blue-700">{c}</span>
              ))}
            </div>
          )}

          {/* K线+两融叠加图 */}
          <div className="rounded-lg border bg-white p-4">
            <div className="flex items-center gap-3 mb-2">
              <h2 className="text-sm font-semibold">K线 + 两融</h2>
              <div className="flex gap-1">
                {(["1m", "3m", "6m", "1y"] as RangeKey[]).map((r) => (
                  <button key={r} onClick={() => setRange(r)} className={`rounded px-2 py-1 text-xs ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>
                    {r === "1m" ? "1月" : r === "3m" ? "3月" : r === "6m" ? "6月" : "1年"}
                  </button>
                ))}
              </div>
            </div>
            {stock.dailyKlines.length > 0 ? (
              <KlineMarginOverlay klines={stock.dailyKlines} margins={stock.dailyMargins} />
            ) : (
              <div className="h-[400px] flex items-center justify-center text-gray-400">无数据</div>
            )}
          </div>
        </>
      )}
    </div>
  );
}